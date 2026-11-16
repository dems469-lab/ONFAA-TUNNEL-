#!/usr/bin/env python3
"""Deterministic end-to-end services for the ONFAA TUNNEL CI test.

Runs two local servers:
  * a token HTTP server bound to a TEST-NET address (192.0.2.10 by default)
    that is added to the host loopback alias by the workflow;
  * a minimal DNS responder for onfaa.test -> 192.0.2.10.

The emulator cannot reach 192.0.2.10 directly. A successful HTTP request from
the Android app therefore proves the packet went through the real Xray client,
the official Xray-core server, and its freedom outbound.
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


def run_dns(bind: str, port: int, answer_ip: str) -> None:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((bind, port))
    log(f"DNS listening on {bind}:{port} (onfaa.test -> {answer_ip})")
    answer = socket.inet_aton(answer_ip)
    while True:
        try:
            packet, peer = sock.recvfrom(1500)
        except OSError as exc:
            log(f"DNS socket error: {exc}")
            return
        parsed = parse_question(packet)
        if parsed is None:
            continue
        question_end, name, qtype, qclass = parsed
        transaction = packet[:2]
        question = packet[12:question_end]
        if name == b"onfaa.test" and qtype == 1 and qclass == 1:
            flags = 0x8180  # response, recursion available, no error
            header = transaction + struct.pack("!HHHHH", flags, 1, 1, 0, 0)
            record = b"\xc0\x0c" + struct.pack("!HHIH", 1, 1, 60, 4) + answer
            sock.sendto(header + question + record, peer)
            log(f"DNS A onfaa.test from {peer[0]}:{peer[1]} -> {answer_ip}")
        else:
            # NXDOMAIN for other names; NOERROR/NODATA for AAAA on onfaa.test.
            rcode = 0 if name == b"onfaa.test" and qtype == 28 else 3
            flags = 0x8180 | rcode
            header = transaction + struct.pack("!HHHHH", flags, 1, 0, 0, 0)
            sock.sendto(header + question, peer)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--http-bind", default="192.0.2.10")
    parser.add_argument("--http-port", type=int, default=18080)
    parser.add_argument("--dns-bind", default="127.0.0.1")
    parser.add_argument("--dns-port", type=int, default=15353)
    parser.add_argument("--answer-ip", default="192.0.2.10")
    parser.add_argument("--token", required=True)
    args = parser.parse_args()

    httpd = ThreadingHTTPServer((args.http_bind, args.http_port), TokenHandler)
    httpd.token = args.token  # type: ignore[attr-defined]
    http_thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    http_thread.start()
    log(f"HTTP token server on {args.http_bind}:{args.http_port}")

    dns_thread = threading.Thread(
        target=run_dns, args=(args.dns_bind, args.dns_port, args.answer_ip), daemon=True
    )
    dns_thread.start()

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
