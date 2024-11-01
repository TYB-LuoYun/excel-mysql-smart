/**
 * 
 */
package top.anets.exception;

/**
 * @author Administrator
 *
 */
public class AlertException extends RuntimeException{
    private String code ;
    private String message ;

	public AlertException() {
		super();
	}

	public AlertException(String message, Throwable cause) {
		super(message, cause);
		this.message=message;
	}

	public AlertException(String message) {
		super(message);
		this.message=message;
	}


	public AlertException(String code , String message) {
		super(message);
		this.code=code;
		this.message=message;
	}

	public AlertException(Throwable cause) {
		super(cause);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
