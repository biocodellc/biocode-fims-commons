package biocode.fims.query.dsl;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ParsedRangeTest {

    @Test
    public void should_throw_exception_for_invalid_range() {
        testInvalidRange("[1]");
        testInvalidRange("[1 TO 10");
        testInvalidRange("{1 TO 10");
        testInvalidRange("1 TO 10]");
        testInvalidRange("1 TO 10}");
        testInvalidRange("1 TO 10");
        testInvalidRange("{* TO *}");
    }

    private void testInvalidRange(String range) {
        try {
            new RangeExpression.ParsedRange(range);
            fail();
        } catch (Exception e) {
            if (e instanceof FimsRuntimeException) {
                assertEquals(QueryCode.INVALID_QUERY, ((FimsRuntimeException) e).getErrorCode());
            } else {
                fail();
            }
        }
    }

    @Test
    public void test_parsing_of_inclusion_range() {
        RangeExpression.ParsedRange parsedRange = new RangeExpression.ParsedRange("[1TO 10]");

        assertEquals(ComparisonOperator.GREATER_THEN_EQUAL, parsedRange.leftOperator());
        assertEquals(ComparisonOperator.LESS_THEN_EQUAL, parsedRange.rightOperator());
        assertEquals("1", parsedRange.leftValue());
        assertEquals("10", parsedRange.rightValue());
    }

    @Test
    public void test_parsing_of_exclusion_range() {
        RangeExpression.ParsedRange parsedRange = new RangeExpression.ParsedRange("{1 TO10}");

        assertEquals(ComparisonOperator.GREATER_THEN, parsedRange.leftOperator());
        assertEquals(ComparisonOperator.LESS_THEN, parsedRange.rightOperator());
        assertEquals("1", parsedRange.leftValue());
        assertEquals("10", parsedRange.rightValue());
    }

    @Test
    public void test_parsing_of_infinity_range() {
        RangeExpression.ParsedRange parsedRange = new RangeExpression.ParsedRange("{* TO 10}");

        assertEquals(null, parsedRange.leftOperator());
        assertEquals(ComparisonOperator.LESS_THEN, parsedRange.rightOperator());
        assertEquals(null, parsedRange.leftValue());
        assertEquals("10", parsedRange.rightValue());

        parsedRange = new RangeExpression.ParsedRange("{1 TO *]");

        assertEquals(ComparisonOperator.GREATER_THEN, parsedRange.leftOperator());
        assertEquals(null, parsedRange.rightOperator());
        assertEquals("1", parsedRange.leftValue());
        assertEquals(null, parsedRange.rightValue());
    }


}