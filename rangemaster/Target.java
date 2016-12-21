package accufiresystems.rangemaster;

import java.util.ArrayList;
import java.util.List;

public class Target {

	public static final List<Target> targets = new ArrayList<Target>();

	private final double percentX;
	private final double percentY;
	private final double percentR;
	private boolean hit = false;

	public Target(final double x, final double y, final double r){
		percentX = x;
		percentY = y;
		percentR = r;
	}

	public static void clearHits() {
		for (final Target t : targets) {
			t.hit = false;
		}
	}

	public boolean isHit() {
		return hit;
	}

	public void setHit(final boolean isHit) {
		hit = isHit;
	}

	public double getX() {
		return percentX;
	}

	public double getY() {
		return percentY;
	}

	public double getR() {
		return percentR;
	}
}
