package mysql_Data;

public class OrderException extends Exception {
	/**
	 class that handles exceptions from the operations performed on the data in database 
	 */
	private static final long serialVersionUID = 1L;
	public String message;
	public int reference;
	public OrderException(String message)
	{
		super(message);
		this.message = message;
	}

	public OrderException(int reference)
	{
		super();
		this.reference = reference;
	}

	@Override
	public String getMessage()
	{
		return message;

	}
	public int getReference()
	{
		return reference;
	}
}
