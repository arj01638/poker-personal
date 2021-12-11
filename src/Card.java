
public class Card implements Comparable<Card> {

	final int val;
	final int suit;
	
	Card(int value, int suit) {
		this.val = value;
		this.suit = suit;
	} // Card
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		switch(val) {
			case 11:
				string.append("J");
				break;
			case 12:
				string.append("Q");
				break;
			case 13:
				string.append("K");
				break;
			case 14:
				string.append("A");
				break;
			default:
				string.append(Integer.toString(val));
		}
		switch(suit) {
			case 1:
				string.append("s");
				break;
			case 2:
				string.append("c");
				break;
			case 3:
				string.append("h");
				break;
			case 4:
				string.append("d");
				break;
		}
		return string.toString();
	}

	@Override
	public int compareTo(Card o) {
		return o.val - this.val;
	}
	
	public int compareToS(Card o) {
		if (o.suit != this.suit) return 1;
		return 0;
	}
	
}
