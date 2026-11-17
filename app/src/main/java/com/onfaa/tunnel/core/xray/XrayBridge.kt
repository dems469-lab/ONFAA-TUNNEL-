package com.onfaa.tunnel.core.xray

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridge to the official libXray gomobile AAR (contract API v3).
 *
 * The AAR is injected by CI and is gitignored, so the generated JVM class is
 * resolved by reflection. Verified entry points (javap, Phase 2B):
 *   libXray.LibXray#invoke(String): String
 *   libXray.LibXray#registerDialerController(libXray.DialerController)
 *   libXray.LibXray#registerListenerController(libXray.DialerController)
 *   libXray.LibXray#setDNS(libXray.DialerController, String)
 *   libXray.LibXray#resetDNS()
 *   libXray.DialerController#protectFd(long): boolean
 *
 * No native call runs on the UI thread. No configuration or credential is logged.
 */
object XrayBridge {
    const val API_VERSION: Int = 3
    private const val MAX_REQUEST_BYTES: Int = 16 * 1024 * 1024

    data class InvokeResponse(
        val success: Boolean,
        val data: JSONObject?,
        val error: String,
        val raw: String
    )

    @Volatile
    private var resolved: Pair<Any?, Method>? = null

    // Go keeps a reference to controller callbacks; hold ours strongly as well.
    @Volatile
    private var controllerRef: Any? = null

    private fun resolveInvoke(): Pair<Any?, Method> {
        resolved?.let { return it }
        synchronized(this) {
            resolved?.let { return it }
            val classNames = listOf(
                "libXray.LibXray",
                "libXray.Libxray",
                "libxray.LibXray",
                "libxray.Libxray"
            )
            val errors = mutableListOf<String>()
            for (name in classNames) {
                try {
                    val cls = Class.forName(name)
                    val method = cls.getMethod("invoke", String::class.java)
                    resolved = null to method
                    return null to method
                } catch (t: Throwable) {
                    errors.add("$name: ${t.javaClass.simpleName}")
                }
            }
            throw IllegalStateException("libXray invoke not found. Tried: ${errors.joinToString("; ")}")
        }
    }

    fun isAvailable(): Boolean = try {
        resolveInvoke()
        true
    } catch (_: Throwable) {
        false
    }

    /**
     * Forces initialization of the gomobile-generated class, whose static
     * initializer loads the single native Go runtime (libgojni.so).
     */
    fun loadNativeLibrary(): Boolean {
        val (_, method) = resolveInvoke()
        val cls = method.declaringClass
        Class.forName(cls.name, true, cls.classLoader)
        return true
    }

    fun invokeRaw(requestJson: String): String {
        val bytes = requestJson.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_REQUEST_BYTES) { "Request exceeds 16 MiB limit" }
        val (target, method) = resolveInvoke()
        val result = method.invoke(target, requestJson)
        return result as? String ?: throw IllegalStateException("Invoke returned non-string result")
    }

    fun invokeParsed(requestJson: String): InvokeResponse {
        val raw = invokeRaw(requestJson)
        val obj = JSONObject(raw)
        val success = obj.optBoolean("success", false)
        val data = obj.optJSONObject("data")
        val error = obj.optString("error", "")
        return InvokeResponse(success = success, data = data, error = error, raw = raw)
    }

    fun buildRequest(method: String, payload: JSONObject? = null): String {
        val root = JSONObject()
        root.put("apiVersion", API_VERSION)
        root.put("method", method)
        if (payload != null) {
            root.put("payload", payload)
        }
        return root.toString()
    }

    suspend fun xrayVersion(): InvokeResponse = withContext(Dispatchers.Default) {
        invokeParsed(buildRequest("xrayVersion"))
    }

    suspend fun invoke(method: String, payload: JSONObject? = null): InvokeResponse =
        withContext(Dispatchers.Default) {
            invokeParsed(buildRequest(method, payload))
        }

    suspend fun convertShareLink(text: String): InvokeResponse =
        invoke("convertShareLinksToXrayJson", JSONObject().put("text", text))

    suspend fun testXray(configJson: String): InvokeResponse =
        invoke("testXray", JSONObject().put("xrayJson", configJson))

    suspend fun runXray(configJson: String): InvokeResponse =
        invoke("runXray", JSONObject().put("xrayJson", configJson))

    suspend fun stopXray(): InvokeResponse = invoke("stopXray")

    suspend fun getXrayState(): Boolean = withContext(Dispatchers.Default) {
        val response = invokeParsed(buildRequest("getXrayState"))
        response.success && response.data?.optBoolean("running") == true
    }

    /**
     * Current VpnService protector. Kept behind a delegate because Xray-core's
     * controller list is append-only (`transport/internet.RegisterDialerController`
     * appends to `internet.Controllers` and every dial must satisfy *all* of
     * them, without any unregister API). Registering a controller per connect
     * would leak dead services, and registering a `false` controller would make
     * every later dial fail with "protect Android VPN socket failed" for the
     * whole life of the process.
     */
    private val protectDelegate = AtomicReference<(Int) -> Boolean>({ false })

    @Volatile
    private var protectorRegistered = false

    /**
     * Installs [protect] as the Android VpnService socket protector. [protect]
     * receives the raw native fd and must return true when
     * VpnService.protect(fd) succeeded.
     *
     * The gomobile controller is registered exactly once per process; later
     * calls only swap the delegate.
     */
    fun registerSocketProtector(protect: (Int) -> Boolean): Boolean = try {
        protectDelegate.set(protect)
        if (!protectorRegistered) {
            synchronized(this) {
                if (!protectorRegistered) {
                    val iface = Class.forName("libXray.DialerController")
                    val proxy = newControllerProxy(iface) { fd -> protectDelegate.get().invoke(fd) }
                    val libClass = resolveInvoke().second.declaringClass
                    libClass.getMethod("registerDialerController", iface).invoke(null, proxy)
                    libClass.getMethod("registerListenerController", iface).invoke(null, proxy)
                    controllerRef = proxy
                    protectorRegistered = true
                }
            }
        }
        true
    } catch (_: Throwable) {
        false
    }

    /**
     * Drops the service-bound protector so the Go runtime does not keep
     * dialing with a destroyed VpnService. It only clears the delegate: the
     * registered controller stays alive (Xray-core cannot unregister it) and
     * rejects dials instead of letting them escape the VPN unprotected.
     */
    fun clearSocketProtector() {
        protectDelegate.set { false }
    }

    fun setDNS(protect: (Int) -> Boolean, server: String) {
        try {
            protectDelegate.set(protect)
            val iface = Class.forName("libXray.DialerController")
            val proxy = newControllerProxy(iface) { fd -> protectDelegate.get().invoke(fd) }
            val libClass = resolveInvoke().second.declaringClass
            libClass.getMethod("setDNS", iface, String::class.java).invoke(null, proxy, server)
            controllerRef = proxy
        } catch (t: Throwable) {
            throw unwrap(t)
        }
    }

    fun resetDNS() {
        try {
            resolveInvoke().second.declaringClass.getMethod("resetDNS").invoke(null)
        } catch (t: Throwable) {
            throw unwrap(t)
        }
    }

    private fun newControllerProxy(iface: Class<*>, protect: (Int) -> Boolean): Any =
        Proxy.newProxyInstance(
            iface.classLoader,
            arrayOf(iface),
            ControllerHandler(protect)
        )

    private class ControllerHandler(private val protect: (Int) -> Boolean) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
            when (method.name) {
                "protectFd" -> try {
                    protect((args?.firstOrNull() as? Number)?.toInt() ?: -1)
                } catch (_: Throwable) {
                    // A Java exception must never propagate into the Go runtime
                    // through the JNI callback: report the socket as unprotected.
                    false
                }
                "toString" -> "OnfaaDialerController"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                // Fail closed, but never throw inside a native callback.
                else -> defaultValue(method.returnType)
            }

        private fun defaultValue(type: Class<*>): Any? = when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Double.TYPE -> 0.0
            java.lang.Float.TYPE -> 0f
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Character.TYPE -> '\u0000'
            else -> null
        }
    }

    private fun unwrap(t: Throwable): Throwable =
        if (t is InvocationTargetException) (t.targetException ?: t) else t

    /** For tests/inspection: class and method name if resolved. */
    fun resolvedSignature(): String? = try {
        val (_, m) = resolveInvoke()
        "${m.declaringClass.name}#${m.name}(String):String"
    } catch (_: Throwable) {
        null
    }
}
