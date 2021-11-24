
public class PokerDriver {

	public static void main(String[] args) {
		PokerMain main = new PokerMain();
		try {
			main.start();
		} catch (Exception e) {
			main.printGlobalString();
			e.printStackTrace();

		}
		
	}

}
