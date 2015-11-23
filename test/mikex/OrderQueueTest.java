/**
 * 
 */
package mikex;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author meng
 *
 */
public class OrderQueueTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	OrderQueue q=new OrderQueue(5);
	/**
	 * Test method for {@link mikex.OrderQueue#OrderQueue(double)}.
	 */
	@Test
	public void testOrderQueue() {
		//fail("Not yet implemented");
		q = new OrderQueue(5);
	}

	/**
	 * Test method for {@link mikex.OrderQueue#add(mikex.Order)}.
	 */
	@Test
	public void testAdd() {
		//fail("Not yet implemented");
		Order o = new Order();
		o.setTimestamp();

		q.add(o);
		
		
	}

	/**
	 * Test method for {@link mikex.OrderQueue#getPrice()}.
	 */
	@Test
	public void testGetPrice() {
		q = new OrderQueue(5);
		assertTrue(q.getPrice()==5);
		q = new OrderQueue(3);
		assertTrue(q.getPrice()==3);
	}

	/**
	 * Test method for {@link mikex.OrderQueue#getVolume()}.
	 */
	@Test
	public void testGetVolume() {
		assertTrue(q.getVolume()==0);
		Order o = new Order();
		o.leavesQty.setValue(5);
		q.add(o);
		assertTrue(q.getVolume()==5);
		Order p = new Order();
		p.leavesQty.setValue(5);
		q.add(p);
		assertTrue(q.getVolume()==10);
	}

	/**
	 * Test method for {@link mikex.OrderQueue#getFirst()}.
	 */
	@Test
	public void testGetFirst() {
		Order o = new Order();
		q.add(o);
		q.getFirst();
		//fail("Not yet implemented");
	}

	/**
	 * Test method for {@link mikex.OrderQueue#del(mikex.Order)}.
	 */
	@Test
	public void testDel() {
		Order o = new Order();
		q.add(o);
		q.del(q.getFirst());
		//fail("Not yet implemented");
	}

	/**
	 * Test method for {@link mikex.OrderQueue#hasContents()}.
	 */
	@Test
	public void testHasContents() {
		assertFalse(q.hasContents());
		Order o = new Order();
		q.add(o);
		assertTrue(q.hasContents());
		q.del(q.getFirst());		
		assertFalse(q.hasContents());
		//fail("Not yet implemented");
	}

}
