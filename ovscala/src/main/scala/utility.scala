
package overlap

case class Now() {
  import java.util.Date
  import java.util.TimeZone
  import java.text.SimpleDateFormat

  val now = new Date()
  def asISO = {
    val tz = TimeZone.getDefault
    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    df.setTimeZone(tz)
    df.format(now)
  }
  def println(s: String) = Console.println("%s: %s" format (asISO, s))
}
