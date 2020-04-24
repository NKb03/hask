/**
 *@author Nikolaus Knop
 */

package hask.core.parse

/**
 * Occurs if an internal in the parser happens
*/
class ParserError(message: String, cause: Throwable? = null): RuntimeException(message, cause)