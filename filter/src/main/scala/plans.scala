package unfiltered.filter

import javax.servlet.{Filter, FilterConfig, FilterChain, ServletRequest, ServletResponse}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import unfiltered.request._
import unfiltered.response._
import unfiltered.Cycle

trait InittedFilter extends Filter {
  private var configVar: FilterConfig = _
  def init(config: FilterConfig) { configVar = config; }
  def config = configVar

  def destroy { }
}

object Plan {
  type Intent = Cycle.Intent[HttpServletRequest,HttpServletResponse]
}

/** Object to facilitate Plan.Intent definitions. Type annotations
 *  are another option. */
object Intent {
  def apply(intent: Plan.Intent) = intent
}

/**
 * Servlet filter that wraps an Intent and adheres to standard filter
 * chain behaviour.
 */
trait Plan extends InittedFilter {
  def intent: Plan.Intent
  def doFilter(request: ServletRequest,
               response: ServletResponse,
               chain: FilterChain) {
    (request, response) match {
      case (hreq: HttpServletRequest, hres: HttpServletResponse) =>
        val request = new RequestBinding(hreq)
        val response = new ResponseBinding(hres)
        Pass.fold(
          intent,
          (_: HttpRequest[HttpServletRequest]) =>
            chain.doFilter(request.underlying, response.underlying),
          (_: HttpRequest[HttpServletRequest], 
           rf: ResponseFunction[HttpServletResponse]) => {
            val res = rf(response)
            res.outputStream.close()
            res
          }
        )(request)
     }
  }
}

/** To define a filter class with an independent function */
@deprecated("Use Planify.apply or extend Plan", "0.6.8")
class Planify(val intent: Plan.Intent) extends Plan
/** To create a filter instance with an independent function */
object Planify {
  def apply(intent: Plan.Intent) = new Planify(intent)
}
