package rocks.zipcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        String input = "(print (divide 6 3))";

        Compiler compiler = new Compiler();

        System.out.println(";; Input code: "+input);

        String output = compiler.compile(input);

        System.out.println(output);
        assertTrue(true);
    }

    String input = "(print (divide 6 3))";

    Compiler compiler = new Compiler();

    System.out.println(";; Input code: "+input);

    String output = compiler.compile(input);

    System.out.println(output);
}
