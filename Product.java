package mysql_Data;

public class Product {
	//class that holds the supplier data, units sold and sale value
	int tracking_id;
	int quantity;
	Double unit_price;
	public Product(int tracking_id, int quantity, Double unit_price) {
		super();
		this.tracking_id = tracking_id;
		this.quantity = quantity;
		this.unit_price = unit_price;
	}


}

