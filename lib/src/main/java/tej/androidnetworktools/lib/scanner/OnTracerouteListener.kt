package tej.androidnetworktools.lib.scanner

import tej.androidnetworktools.lib.Route

interface OnTracerouteListener {
    fun onRouteAdd(route: Route?)
    fun onComplete(routes: List<Route?>?)
    fun onFailed()
}
