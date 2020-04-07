package mysql_Data;

import java.sql.ResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



public class Operations extends ConnectDatabase implements inventoryControl{

	//class that performs database operations and maintains supplier orders data in the database

	@Override
	public void Ship_order(int orderNumber) throws OrderException {

		//Method that ships the given order number and reduces the inventory as per the ordered quantity

		connectDB(); //connect to the database to fetch the data

		try {

			//check if the given order id exists in the orders table
			String order_que = "select * from orders where orderid = " + orderNumber + " ;";
			ResultSet order = connect.createStatement().executeQuery(order_que);
			if (!order.next())
			{
				throw new OrderException("order id does not exist");
			}

			//query to fetch the shipped date of the given order id
			String shp_que = "select ShippedDate from orders where orderid = " + orderNumber + " ;";
			ResultSet res = connect.createStatement().executeQuery(shp_que);

			while(res.next())
			{

				//ship the order whose shipped date is null

				if (res.getString("ShippedDate") == null)
				{
					//update the shipped date a day less, for the order id based on the required date
					String update_shp_dt = " update orders set ShippedDate = RequiredDate -1 where orderID = " + orderNumber;
					connect.createStatement().executeUpdate(update_shp_dt);

					//fetch all the products which are discontinued under the given order id
					String query = "select products.productid, productName, UnitsInStock, Quantity from orderdetails " + 
							"inner join products " + 
							"on orderdetails.productid = products.productid " + 
							"where Discontinued = 0 and orderid = " + orderNumber + "; ";
					ResultSet ship_result = connect.createStatement().executeQuery(query);

					while(ship_result.next())
					{
						//if units in stock are less than the required quantity
						if (ship_result.getInt("UnitsInStock") > ship_result.getInt("Quantity"))
						{
							//subtracting the quantity asked from the units in stock and updating the inventory
							int Unitsinstock = ship_result.getInt("UnitsInStock") - ship_result.getInt("Quantity");
							String update_query = "update products set Unitsinstock = " +
									Unitsinstock + " where ProductID = " + ship_result.getInt("ProductID");
							connect.createStatement().executeUpdate(update_query);
						}
					}
					//closing the resultset and connection
				}
				else
					throw new OrderException("The order id is already shipped");
			}
			//closing the resultset and connection

			res.close();
			connect.close();

		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(OrderException e)
		{
			System.out.println(e.getMessage());
		}
		finally {


			// Closing the statements, and connections that are open and
			// holding resources.
			try {
				if (statement != null) {
					statement.close();
				}

				if (connect != null) {
					connect.close();
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	private void arrange_data(ResultSet resultSet, String date,Map<Integer,Double> price_map, Map<Integer, ArrayList<Integer>> mp, Map<Integer, ArrayList<Product>> mp2) throws SQLException {
		// Method that arranges the fetched data into two maps 
		//containing supplier - product relation
		// and product - details relation


		while(resultSet.next())
		{
			//insert into supplier orders with the supplier id and OrderDate (given as input in function)
			String ins = "insert into supplier_orders (SupplierID,OrderDate) values (" 
					+ resultSet.getInt("SupplierID") + "," + "'" + date + "'" + ");";
			connect.createStatement().executeUpdate(ins);

			//fetching the allocated tracking id
			String que = "select TrackingID from supplier_orders where SupplierID = "
					+ resultSet.getInt("SupplierID") + " and OrderDate = "
					+ "'" + date + "'" + " ;";
			ResultSet rs = connect.createStatement().executeQuery(que);
			int Tracking_ID = 0;
			while(rs.next())
			{
				Tracking_ID = rs.getInt("TrackingID");
			}

			//organizing the data into two maps
			if (mp.containsKey(resultSet.getInt("SupplierID")))
			{
				ArrayList<Integer> alt = mp.get(resultSet.getInt("SupplierID"));
				alt.add(resultSet.getInt("ProductID"));
				mp.put(resultSet.getInt("SupplierID"), alt);
				ArrayList<Product> ar2 = new ArrayList<Product>();
				int track = Tracking_ID;
				int reorderlvl = resultSet.getInt("ReorderLevel");
				Double price = price_map.get(resultSet.getInt("ProductID"));
				Product pd = new Product(track,reorderlvl,price); //storing product details
				ar2.add(pd);
				mp2.put(resultSet.getInt("ProductID"), ar2);
			}
			else
			{
				ArrayList<Integer> arr = new ArrayList<>();
				arr.add(resultSet.getInt("ProductID"));
				mp.put(resultSet.getInt("SupplierID"), arr);
				ArrayList<Product> ar2 = new ArrayList<Product>();
				int track = Tracking_ID;
				int reorderlvl = resultSet.getInt("ReorderLevel");
				Double price = price_map.get(resultSet.getInt("ProductID"));
				Product pd = new Product(track,reorderlvl,price);
				ar2.add(pd);
				mp2.put(resultSet.getInt("ProductID"), ar2);
			}
		}
	}


	public Map<Integer,Double> set_price(ResultSet price_resultSet, String date, Map<Integer, ArrayList<Integer>> supplier_prod, Map<Integer, ArrayList<Product>> prod_details)
	{
		//Method that fetches the last sale price of the order placed and stores in a map data structure

		Map<Integer,Double> price = new HashMap<>();


		try {
			while(price_resultSet.next())
			{
				if (!price.containsKey(price_resultSet.getInt("ProductID")))
				{				

					price.put(price_resultSet.getInt("ProductID"),price_resultSet.getDouble("UnitPrice"));

				}
			}

			//fetching all the products whose units in stock < reorder level
			String fetch_query = "select * from products "
					+ "where UnitsInStock <= ReorderLevel and Discontinued = 0 ; ";


			ResultSet ft_resultSet = connect.createStatement().executeQuery(fetch_query);

			//arrange data into two maps and map the product to its respective unit price

			arrange_data(ft_resultSet,date,price,supplier_prod,prod_details);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return price;
	}


	@Override
	public int Issue_reorders(int year, int month, int day) {
		// Method to issue reorders based on the given date
		connectDB();   //connects to the database to fetch the required data

		String date = year + "-" + month + "-" + day;     

		Map<Integer,ArrayList<Integer>> supplier_prod = new HashMap<>(); //stores supplier and product details
		Map<Integer,ArrayList<Product>> prod_details = new HashMap<>();  //stores product details


		try {

			//checks if the reorder is already placed on the same day
			String sort_que = "select count(*) as count from supplier_orders where OrderDate = " + "'" + date + "' ;" ;
			ResultSet val = connect.createStatement().executeQuery(sort_que);
			while(val.next())
			{
				//if no reorders are placed on the same day, then a reorder is issued
				if (val.getInt("count") == 0)
				{

					//fetching all the product details 
					String fetch_price = "select * from orderdetails where productId in (select ProductID " + 
							"from products where UnitsInStock <=ReorderLevel and Discontinued = 0) order by orderId DESC ;";
					ResultSet price_resultSet = connect.createStatement().executeQuery(fetch_price);

					//Assigning the last sale price to each product
					set_price(price_resultSet,date,supplier_prod,prod_details);

					for(Integer supplier_id : supplier_prod.keySet())
					{
						ArrayList<Integer> prod_id = supplier_prod.get(supplier_id); //array list to store different categories

						for(int lst=0;lst<prod_id.size();lst++)
						{
							ArrayList<Product> pd = prod_details.get(prod_id.get(lst)); //array list to store summary data
							Double unitprice = pd.get(0).unit_price - (pd.get(0).unit_price * 0.15)  ; //15% markup on the price

							//insert the fetched details into the supplier_product_details table with quantity and unit price
							String ins_que = "insert into supplier_product_details "
									+ "(TrackingID,ProductID,Quantity,UnitPrice) "
									+ "values( "
									+ pd.get(0).tracking_id + ", "      
									+ supplier_id + ", "
									+ 2* pd.get(0).quantity + ", "      //double the reorder level
									+ unitprice                           	                          
									+ ");";

							connect.createStatement().executeUpdate(ins_que);
						}
					}

					//fetch the number of suppliers on that particular day
					String query = "select count(Distinct SupplierID) as suppliers from supplier_orders where OrderDate = " + "'" + date + "'" + " ;";
					ResultSet sup_resultSet = connect.createStatement().executeQuery(query);

					int count = 0;
					while (sup_resultSet.next())
					{

						count = sup_resultSet.getInt("suppliers");
					}
					sup_resultSet.close();
					return count;
				}
			}
			val.close();
		}

		catch (SQLException e) {
			e.printStackTrace();
		}
		finally {

			// Closing the statements, and connections that are open and
			// holding resources.
			try {
				if (statement != null) {
					statement.close();
				}

				if (connect != null) {
					connect.close();
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		return 0;
	}

	@Override
	public void Receive_order(int internal_order_reference) throws OrderException {

		// Method that delivers the order based on the given internal_order_reference given as input
		// updates the inventory with the new stock 

		connectDB(); //connect to the database to fetch the data

		try {
			//check if the given reference id exists in the supplier orders table
			String order_que = "select * from supplier_orders where trackingid = " + internal_order_reference + " ;";
			ResultSet order = connect.createStatement().executeQuery(order_que);
			if (!order.next())
			{
				throw new OrderException(internal_order_reference);
			}

			//fetch the current date of the system
			long data=System.currentTimeMillis();  
			java.sql.Date current_date=new java.sql.Date(data);  

			//query to update the arrival date of the products with the given reference id
			String upd_que = " update supplier_orders " + 
					"set ArrivalDate = " + "'" + current_date + "'" +
					" where TrackingID = " + internal_order_reference +
					" and ArrivalDate is null;" ; 
			int update_date = connect.createStatement().executeUpdate(upd_que);

			//throwing an exception if the reference does not exist or none of the records are updated
			if(update_date == 0)
				throw new OrderException(internal_order_reference);
			else
			{
				//query to update the product table with the new inventory details with respect to units in stock
				String upd_prod = "update products " + 
						"inner join supplier_product_details "
						+ "on products.ProductID = supplier_product_details.ProductId" + 
						" inner join  supplier_orders "
						+ "on supplier_orders.trackingId = supplier_product_details.trackingId" + 
						" set products.UnitsInStock = products.UnitsInStock + supplier_product_details.Quantity " + 
						" where supplier_orders.trackingId = " +  internal_order_reference
						+ " and supplier_orders.ArrivalDate is not null;";
				int update_prod = connect.createStatement().executeUpdate(upd_prod);

				//throwing an exception if none of the products inventory are updated
				if(update_prod == 0)
					throw new OrderException(internal_order_reference);
			}

			connect.close();


		}catch (OrderException oe) {
			System.out.println(oe.getReference());
		} 

		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		finally {


			// Closing the statements, and connections that are open and
			// holding resources.
			try {
				if (statement != null) {
					statement.close();
				}

				if (connect != null) {
					connect.close();
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

	}

}
