package test.acgen;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.agent.webapp.client.ActionClient;
import com.axway.ats.agent.core.exceptions.AgentException;


import com.axway.ats.agent.core.ant.needed_in_acgen_tests.TestType;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;



@PublicAtsApi
public class ComplexActionClass extends ActionClient {

    private static final String COMPONENT_NAME = "agenttest";
    

    

    public static final int INT_CONSTANT = 1;

    public static final String STRING_CONSTANT = "one";

    public static final int[] INT_CONSTANTS = { 1,
2,
3,
4,
5,
6,
7,
8};

    public static final String[] STRING_CONSTANTS = { "one",
"one",
"two",
"three",
"four"};

    public static final SomeTestEnum ENUM_CONSTANT_ONE = ONE;

    public static final SomeTestEnum ENUM_CONSTANT_TWO = TWO;

    public static final SomeTestEnum[] ENUM_CONSTANTS = { ONE,
TWO,
THREE};


    @PublicAtsApi
    public ComplexActionClass() {

        super( ActionClient.LOCAL_JVM, COMPONENT_NAME );
        

    }

    @PublicAtsApi
    public ComplexActionClass( String host ) {

        super( host, COMPONENT_NAME );
        

    }
    /**
     * This method returns array of int
     * @return int array
     * @throws AgentException  if an error occurs during action execution
     */
    
    @PublicAtsApi
    public int[] call1(  ) throws AgentException {

        return ( int[] ) executeAction( "Complex Action Class call1", new Object[]{  }   );
    }
    /**
     * returns {@link Map} with {@link String} keys and {@link TestType} values
     * @return Map with strings and testTypes values
     * @throws AgentException  if an error occurs during action execution
     */
    
    @PublicAtsApi
    public Map<String, TestType> call2(  ) throws AgentException {

        return ( Map<String, TestType> ) executeAction( "Complex Action Class call2", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public List<Calendar> call3(  ) throws AgentException {

        return ( List<Calendar> ) executeAction( "Complex Action Class call3", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public List<Integer> call4(  ) throws AgentException {

        return ( List<Integer> ) executeAction( "Complex Action Class call4", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public Long call5(  ) throws AgentException {

        return ( Long ) executeAction( "Complex Action Class call5", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public String call6(  ) throws AgentException {

        return ( String ) executeAction( "Complex Action Class call6", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public Date call7(  ) throws AgentException {

        return ( Date ) executeAction( "Complex Action Class call7", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public Map<TestType, Locale> call9(  ) throws AgentException {

        return ( Map<TestType, Locale> ) executeAction( "Complex Action Class call9", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public void call8(  ) throws AgentException {

        executeAction( "Complex Action Class call8", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public TestType getmytype2(  ) throws AgentException {

        return ( TestType ) executeAction( "Complex Action Class getMyType2", new Object[]{  }   );
    }
    /**
     * comment for action2
     * @throws AgentException  if an error occurs during action execution
     */
    
    @PublicAtsApi
    public void action2(  ) throws AgentException {

        executeAction( "Complex Action Class action2", new Object[]{  }   );
    }
    /** * //*  /* //**  ////**  //**  comment for action3 
     * @throws AgentException  if an error occurs during action execution
     */
    
    @PublicAtsApi
    public void action3(  ) throws AgentException {

        executeAction( "Complex Action Class action3", new Object[]{  }   );
    }
    /**
     * comment for action4
     * @throws AgentException  if an error occurs during action execution
     */
    
    @PublicAtsApi
    public void action4(  ) throws AgentException {

        executeAction( "Complex Action Class action4", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public void action5(  ) throws AgentException {

        executeAction( "Complex Action Class action5", new Object[]{  }   );
    }
    /**
     * comment for action4
     * @throws AgentException  if an error occurs during action execution
    3232*/
    
    @PublicAtsApi
    public void action6(  ) throws AgentException {

        executeAction( "Complex Action Class action6", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public TestType[] call10(  ) throws AgentException {

        return ( TestType[] ) executeAction( "Complex Action Class call10", new Object[]{  }   );
    }
    
    @PublicAtsApi
    public TestType getmytype(  ) throws AgentException {

        return ( TestType ) executeAction( "Complex Action Class getMyType", new Object[]{  }   );
    }

}
