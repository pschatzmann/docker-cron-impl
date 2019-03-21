package ch.pschatzmann.docker.jobs.tests;

import org.junit.Assert;
import org.junit.Test;

import ch.pschatzmann.docker.jobs.model.Utils;


public class TestUtils {
	@Test
	public void testIsEmpty() throws Exception {
		Assert.assertTrue(Utils.isEmpty(null));
		Assert.assertTrue(Utils.isEmpty(""));
		Assert.assertTrue(Utils.isEmpty(" "));
		Assert.assertFalse(Utils.isEmpty("."));
	}
	
	@Test
	public void testToString() throws Exception {
		String sa[] = {"a","b","c"};
		Assert.assertEquals("a, b, c",Utils.toString(sa, ", "));
	}

}
