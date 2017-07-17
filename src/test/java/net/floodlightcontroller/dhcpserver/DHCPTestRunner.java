package net.floodlightcontroller.dhcpserver;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.Suite;

/**
 * @author Qing Wang (qw@g.clemson.edu) on 7/16/17.
 */
public class DHCPTestRunner {

    @Test
    public void runDHCPTest() throws Exception {
        Result result = JUnitCore.runClasses(DHCPTestSuite.class);
        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
        }
        System.out.println(result.wasSuccessful());
    }

}


@RunWith(Suite.class)
@Suite.SuiteClasses({
        DHCPBindingTest.class,
        DHCPPoolTest.class
})
class DHCPTestSuite {
    // This class remains empty
    // used only as a holder for the above annotations
}
