#!/usr/bin/env python3
"""Deterministic end-to-end services for the ONFAA TUNNEL CI test.

Runs three local servers:
  * a token HTTP server bound to the host loopback (127.0.0.1:18080). The
    official Xray-core server redirects the TEST-NET destination
    (192.0.2.10:18080) to this listener with a freedom outbound override;
  * a minimal UDP DNS responder for onfaa.test -> 192.0.2.10;
  * the same responder over DNS-over-TCP (RFC 7766), because the Android
    emulator's 10.0.2.2 host-loopback alias is reliable for TCP while UDP
    forwarding to the host loopback is not guaranteed.

Nothing listens on 192.0.2.10:18080 itself, and that TEST-NET address is not
reachable from the emulator's default route. A successful HTTP request from
the Android app therefore proves the packet went through the real Xray client,
the official Xray-core server, and its redirecting freedom outbound.
"""

from __future__ import annotations

import argparse
import signal
import socket
import struct
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def log(message: str) -> None:
    print(f"{time.strftime('%H:%M:%S')} {message}", flush=True)


class TokenHandler(BaseHTTPRequestHandler):
    server_version = "OnfaaE2E/1.0"

    def do_GET(self) -> None:  # noqa: N802 (http.server API)
        if self.path.split("?", 1)[0] != "/token":
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"not found")
            return
        body = self.server.token.encode("utf-8")  # type: ignore[attr-defined]
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        log(f"HTTP 200 /token from {self.client_address[0]}:{self.client_address[1]}")

    def log_message(self, fmt: str, *args: object) -> None:
        log(f"HTTP {self.client_address[0]}:{self.client_address[1]} {fmt % args}")


def parse_question(packet: bytes) -> tuple[int, bytes, int, int] | None:
    if len(packet) < 17:
        return None
    offset = 12
    labels: list[bytes] = []
    while offset < len(packet):
        length = packet[offset]
        if length == 0:
            offset += 1
            break
        if length & 0xC0:
            return None
        offset += 1
        if offset + length > len(packet):
            return None
        labels.append(packet[offset : offset + length])
        offset += length
    if offset + 4 > len(packet):
        return None
    qtype, qclass = struct.unpack("!HH", packet[offset : offset + 4])
    qend = offset + 4
    name = b".".join(labels).decode("ascii", "ignore").lower()
    return qend, name.encode("ascii", "ignore"), qtype, qclass


def build_response(packet: bytes, answer_ip: str) -> tuple[bytes, str] | None:
    """Returns the DNS response and a short description, or None if malformed."""
    parsed = parse_question(packet)
    if parsed is None:
        return None
    question_end, name, qtype, qclass = parsed
    transaction = packet[:2]
    question = packet[12:question_end]
    if name == b"onfaa.test" and qtype == 1 and qclass == 1:
        flags = 0x8180  # response, recursion available, no error
        header = transaction + struct.pack("!HHHHH", flags, 1, 1, 0, 0)
        record = b"\xc0\x0c" + struct.pack("!HHIH", 1, 1, 60, 4) + socket.inet_aton(answer_ip)
        return header + question + record, f"A onfaa.test -> {answer_ip}"
    # NXDOMAIN for other names; NOERROR/NODATA for AAAA on onfaa.test.
    rcode = 0 if name == b"onfaa.test" and qtype == 28 else 3
    flags = 0x8180 | rcode
    header = transaction + struct.pack("!HHHHH", flags, 1, 0, 0, 0)
    return header + question, f"qtype={qtype} name={name.decode('ascii', 'ignore')} rcode={rcode}"


def run_dns_udp(bind: str, port: int, answer_ip: str) -> None:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((bind, port))
    log(f"DNS/UDP listening on {bind}:{port} (onfaa.test -> {answer_ip})")
    while True:
        try:
            packet, peer = sock.recvfrom(1500)
        except OSError as exc:
            log(f"DNS/UDP socket error: {exc}")
            return
        result = build_response(packet, answer_ip)
        if result is None:
            continue
        response, description = result
        sock.sendto(response, peer)
        log(f"DNS/UDP {description} from {peer[0]}:{peer[1]}")


def receive_exact(connection: socket.socket, length: int) -> bytes | None:
    data = bytearray()
    while len(data) < length:
        chunk = connection.recv(length - len(data))
        if not chunk:
            return None
        data.extend(chunk)
    return bytes(data)


def handle_dns_tcp_connection(connection: socket.socket, peer: tuple[str, int], answer_ip: str) -> None:
    log(f"DNS/TCP connection from {peer[0]}:{peer[1]}")
    with connection:
        connection.settimeout(10)
        try:
            while True:
                header = receive_exact(connection, 2)
                if header is None:
                    log(f"DNS/TCP client {peer[0]}:{peer[1]} closed before sending a query")
                    return
                length = struct.unpack("!H", header)[0]
                packet = receive_exact(connection, length)
                if packet is None:
                    return
                result = build_response(packet, answer_ip)
                if result is None:
                    return
                response, description = result
                connection.sendall(struct.pack("!H", len(response)) + response)
                log(f"DNS/TCP {description} from {peer[0]}:{peer[1]}")
        except OSError as exc:
            log(f"DNS/TCP connection {peer[0]}:{peer[1]} closed: {exc}")


def run_dns_tcp(bind: str, port: int, answer_ip: str) -> None:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((bind, port))
    sock.listen(16)
    log(f"DNS/TCP listening on {bind}:{port} (onfaa.test -> {answer_ip})")
    while True:
        try:
            connection, peer = sock.accept()
        except OSError as exc:
            log(f"DNS/TCP accept error: {exc}")
            return
        threading.Thread(
            target=handle_dns_tcp_connection,
            args=(connection, peer, answer_ip),
            daemon=True,
        ).start()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--http-bind", default="127.0.0.1")
    parser.add_argument("--http-port", type=int, default=18080)
    parser.add_argument("--dns-bind", default="0.0.0.0")
    parser.add_argument("--dns-port", type=int, default=15353)
    parser.add_argument("--answer-ip", default="192.0.2.10")
    parser.add_argument("--token", required=True)
    args = parser.parse_args()

    httpd = ThreadingHTTPServer((args.http_bind, args.http_port), TokenHandler)
    httpd.token = args.token  # type: ignore[attr-defined]
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    log(f"HTTP token server on {args.http_bind}:{args.http_port}")

    threading.Thread(
        target=run_dns_udp,
        args=(args.dns_bind, args.dns_port, args.answer_ip),
        daemon=True,
    ).start()
    threading.Thread(
        target=run_dns_tcp,
        args=(args.dns_bind, args.dns_port, args.answer_ip),
        daemon=True,
    ).start()

    stopping = False

    def stop(_signum: int, _frame: object) -> None:
        nonlocal stopping
        stopping = True

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)

    while not stopping:
        time.sleep(0.5)
    log("stopping E2E services")
    httpd.shutdown()
    return 0


if __name__ == "__main__":
    sys.exit(main())
