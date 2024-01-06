package runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Scanner;
import java.util.Stack;


public class Runtime {
	// data fields
	private ArrayList<Instruction> pgm;
	private int pc;
	private Stack<Double> stack;
	private ArrayList<Double> memory;
	
	Runtime() {
		stack = new Stack<>();
		memory = new ArrayList<>(10);	
	}
	
	/**
	 * Set SBM to initial state
	 */
	private void initialize() {
		// empty stack
		while (!stack.isEmpty()) {
			stack.pop();
		}
		// reset all memory locations
		for (int i=0; i<10; i++) {
			memory.add(0.0);
		}
		// set pc to first instruction
		pc=1;
	}
	
	/**
	 * Finds if a label has been declared and, if it has, finds the line of code it is on
	 * @param label
	 * @return The code line where the labelName is declared
	 * @throws IllegalStateException if the label has not been declared
	 */
    private int jumpToLabel(String label) {
    	 for (int i = 0; i < pgm.size(); i++) {
    		 if (pgm.get(i).mnemonic.equals("label") && ((Label) pgm.get(i)).getName().equals(label)) { //if the right label
    			 return i + 1; //return pc (i + 1 because it starts at 1)
    		 }
    	 }
    	 throw new IllegalStateException("runtime: label not found"); //if label is not there
	}
	
    
	/**
	 * Processes all instructions and performs correct updates to memory, stack, and program counter
	 * @param i The instruction currently being processed by the function
	 * @return True if the program continues or false if exit instruction
	 * @throws EmptyStackException if Stack is empty at the beginning of, or during, any instructions
	 */
    private boolean processInstruction(Instruction i) {
		switch(i.code) {
		case 0: //exit
			pc++;
			return false;
		case 1: //push literal
			PushLiteral tempPushLit = (PushLiteral) i;
			stack.push(tempPushLit.getLiteral());
			break;
		case 2: //push location
			PushLocation tempPushLoc = (PushLocation) i;
			Double toStack = memory.get(tempPushLoc.getAddress());
			stack.push(toStack);
			break;
		case 3: //pop
			if (stack.isEmpty()) throw new EmptyStackException();
			Pop tempPop = (Pop) i;
			Double toMem = stack.pop();
			memory.set(tempPop.getAddress(), toMem);
			break;
		case 4: //add
			Double[] tempAdd = new Double[2];
			for (int j = 0; j < 2; j++) {
				if (stack.isEmpty()) throw new EmptyStackException();
				tempAdd[j] = stack.pop();
			}
			Double sum = tempAdd[0] + tempAdd[1];
			stack.push(sum);
			break;
		case 5: //subtract
			Double[] tempSub = new Double[2];
			for (int j = 0; j < 2; j++) {
				if (stack.isEmpty()) throw new EmptyStackException();
				tempSub[j] = stack.pop();
			}
			Double diff = tempSub[0] - tempSub[1];
			stack.push(diff);
			break;
		case 6: //multiply
			Double[] tempMul = new Double[2];
			for (int j = 0; j < 2; j++) {
				if (stack.isEmpty()) throw new EmptyStackException();
				tempMul[j] = stack.pop();
			}
			Double product = tempMul[0] * tempMul[1];
			stack.push(product);
			break;
		case 7: //divide
			Double[] tempDiv = new Double[2];
			for (int j = 0; j < 2; j++) {
				if (stack.isEmpty()) throw new EmptyStackException();
				tempDiv[j] = stack.pop();
			}
			Double quotient = tempDiv[0] / tempDiv[1];
			stack.push(quotient);
			break;
		case 8: //label
			//already processed, as Label was declared in parseIntruction, which ran before run() (which runs this function)
			break;
		case 9: //jmpz
			if (stack.peek() == 0) {
				Jmpz tempJmpz = (Jmpz) i;
				pc = jumpToLabel(tempJmpz.getTargetLabel());
			}
			break;
		case 10: //jmp
			Jmp tempJmp = (Jmp) i;
			pc = jumpToLabel(tempJmp.getTargetLabel());
			break;
		case 11: //dec
			if (stack.isEmpty()) throw new EmptyStackException();
			Double decrementTime = stack.pop();
			stack.push(decrementTime - 1);
			break;
		default:
			System.out.println("No way you got here. This is unreachable (hopefully)");
			return false; //in case someone breaks it and it isn't one of the commands
		}
			
		pc++;
		return true;
	}
	
    
    /**
     * Runs the function (initializes the data structures, processes all instructions, and stops when the exit instruction is reached)
     */
	public void run() {
		initialize();
		
		ArrayList<Instruction> holder = new ArrayList<Instruction>();
		for (int i = 0; i < pgm.size(); i++) {
			holder.add(pgm.get(i));
		}
		
		while (pc <= pgm.size()) {
			boolean moveOn = processInstruction(pgm.get(pc - 1));
			if (moveOn == false) return;
		}
		//fill arrayList with instructions by parsing each one and adding that to list each time
		//Once arrayList is full (already might be), process each instruction and if it becomes false, quit
		//I think I have to make each function (so if instruction says add then I make the add function)
		//Yeah bc each class is literally just so it can be recognized as its own thing
	}
	
	
	private Instruction parseInstruction(String str, int line) {
		String[] p = str.split("[ ]+"); // delimiters are non-empty sequences of spaces
		Instruction i=null;

		switch (p[0]) {
		case "exit":
			i = new Exit(0,"exit");
			break;
		case "push": 
			if (p.length==1) {
				throw new IllegalStateException("parseInstruction: syntax error at line "+line);
			}
			try {
			if (p[1].charAt(0)=='m') {
				int loc = Integer.parseInt(p[1].substring(1));
				if (loc<0 || loc>9) {
					throw new IllegalStateException("parseInstruction: syntax error at line "+line);
				}
				i = new PushLocation(2,"push",loc);
			} else {
				i = new PushLiteral(1,"push",Double.parseDouble(p[1]));
			}
			} catch (NumberFormatException e) {
				throw new IllegalStateException("parseInstruction: syntax error at line "+line);
			}
			break;
		case "pop": 
			if (p.length==1) {
				throw new IllegalStateException("parseInstruction: syntax error at line "+line);
			}
			int loc = Integer.parseInt(p[1].substring(1));
			if (loc<0 || loc>9) {
				throw new IllegalStateException("parseInstruction: syntax error at line "+line);
			}
			i = new Pop(3,"pop",loc);
			break;
		case "add": 
			i = new Add(4,"add");
			break;
		case "sub": 
			i = new Sub(5,"sub");
			break;
		case "mul": 
			i = new Mul(6,"mul");
			break;
		case "div": 
			i = new Div(7,"div");
			break;
		case "label":
			i = new Label(8,"label",p[1]);
			break;
		case "jmpz":
			i = new Jmpz(9,"jmpz",p[1]);
			break;
		case "jmp":
			i = new Jmp(10,"jmp",p[1]);
			break;
		case "dec":
			i = new Dec(11,"dec");
			break;
		case "":
			break;
		default:
			throw new IllegalStateException("parseInstruction: syntax error at line "+line);
		}
		return i;
	}
	
	public void readFromFile(String name)  {
		pgm = new ArrayList<>();
		File f = new File(name);
		try {
			Scanner s = new Scanner(f);
			int line=1;
			
			while (s.hasNext()) {
				Instruction i = parseInstruction(s.nextLine(),line);
				if (i!=null) {
					pgm.add(i);
				}
				line++;
			}
			s.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String toString() {
		StringBuilder r = new StringBuilder();
		r.append("Pgm   : ");
		r.append(pgm==null?"null":pgm.toString());
		r.append("\nPc    : "+pc);
		r.append("\nStack : ");
		r.append(stack.toString());
		r.append("\nMemory: ");
		r.append(memory.toString());
		r.append("\n------------------------------------------------\n");
		
		return r.toString();
	}
	
	public static void main(String[] args) {
		Runtime r = new Runtime();
		r.readFromFile("eg1.pgm"); // Load and parse mini-bytecode program
		r.run(); // execute program
		System.out.println(r);  // print resulting state of the SBM
	}
}
