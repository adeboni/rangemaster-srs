package accufiresystems.rangemaster;

import java.util.ArrayList;
import java.util.List;


public class Shot {

	public static final List<Shot> shots = new ArrayList<Shot>();

	private final double percentX;
	private final double percentY;
	private final long time;

	public Shot(final double x, final double y, final long nanos){
		percentX = x;
		percentY = y;
		time = nanos;
	}

	public long getTime() {
		return time;
	}

	public double getX() {
		return percentX;
	}

	public double getY() {
		return percentY;
	}

	public static double getGroupSize(final double[] calData, final List<Shot> shotData) {
		double max = 0.0;
		for (int i = 0; i < shotData.size(); i++) {
			for (int j = 0; j < shotData.size(); j++) {
				final double dist = PointUtils.getDist(shotData.get(i).percentX, shotData.get(i).percentY,
						shotData.get(j).percentX, shotData.get(j).percentY);
				if (dist > max) {
					max = dist;
				}
			}
		}
		final double cal = PointUtils.getDist(calData[0], calData[1], calData[2], calData[3]);
		return max/cal*10;
	}

	public static double getDistFromCenter(final double[] calData, final double[] calCent) {
		final double cal = PointUtils.getDist(calData[0], calData[1], calData[2], calData[3]);
		double sumX = 0;
		double sumY = 0;
		for (final Shot shot : shots) {
			sumX += shot.percentX;
			sumY += shot.percentY;
		}
		return PointUtils.getDist(calCent[0], calCent[1], sumX/shots.size(), sumY/shots.size())/cal*10;
	}

}
