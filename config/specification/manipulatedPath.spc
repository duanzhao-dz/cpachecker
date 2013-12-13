CONTROL AUTOMATON ManipulatedPath

INITIAL STATE ARG1;

STATE USEFIRST ARG1 :
    MATCH "" -> GOTO ARG2_1_1;
STATE USEFIRST ARG2_0_1 :
    MATCH "" -> GOTO ARG2_1_1;
STATE USEFIRST ARG2_1_1 :
    MATCH "void main()" -> GOTO ARG2_2_1;
STATE USEFIRST ARG2_2_1 :
    MATCH "" -> GOTO ARG2_3_1;
STATE USEFIRST ARG2_3_1 :
    MATCH "int y;" -> GOTO ARG2_4_1;
STATE USEFIRST ARG2_4_1 :
    MATCH "int x=5;" -> GOTO ARG2;
    TRUE -> STOP;

STATE USEFIRST ARG2 :
    MATCH "[y != x]" -> STOP;
    TRUE -> GOTO ARG6;

//STATE USEFIRST ARG2 :
//    MATCH "![y != x]" -> GOTO ARG6;
//    TRUE -> STOP;

STATE USEFIRST ARG3 :
    MATCH "Goto: ERROR" -> ERROR;
    TRUE -> STOP;

STATE USEFIRST ARG6 :
	MATCH {abort($?)} || MATCH {exit($?)}  -> STOP;
    TRUE -> GOTO ARG6;
    //MATCH EXIT -> STOP;

END AUTOMATON
