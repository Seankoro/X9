package dk.itu.moapd.x9.s25134

// Route constants for navigation, easily extensible by adding new routes here.
object NavRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val REPORTS = "reports"
    const val REPORTS_ROUTE = "reports?myReportsOnly={myReportsOnly}&criticalOnly={criticalOnly}"
    const val ADD = "add"
    const val DETAIL_ROUTE = "detail/{reportId}"
    const val EDIT_ROUTE = "edit/{reportId}"
    const val MAP = "map"
    const val PROFILE = "profile"

    fun detail(reportId: String) = "detail/$reportId"
    fun edit(reportId: String) = "edit/$reportId"
    fun reports(myReportsOnly: Boolean = false, criticalOnly: Boolean = false): String =
        if (!myReportsOnly && !criticalOnly) REPORTS
        else "$REPORTS?myReportsOnly=$myReportsOnly&criticalOnly=$criticalOnly"
}
