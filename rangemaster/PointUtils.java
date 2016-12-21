package accufiresystems.rangemaster;

import java.awt.Point;

public class PointUtils {

	public static double getSlope(final double x1, final double y1, final double x2, final double y2) {
		return (y2-y1)/(x2-x1);
	}

	public static double getConst(final double x, final double y, final double m) {
		return y - m*x;
	}

	public static double getDist(final double x1, final double y1, final double x2, final double y2) {
		return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
	}

	public static Point[] getPerpLine(final double x1, final double y1, final double x2, final double y2, final double d) {
		final Point ret[] = new Point[2];
		double dx = x1-x2;
		double dy = y1-y2;
		final double dist = Math.sqrt(dx*dx + dy*dy);
		dx /= dist;
		dy /= dist;
		ret[0] = new Point((int)(x1 + (d/2)*dy), (int)(y1 - (d/2)*dx));
		ret[1] = new Point((int)(x1 - (d/2)*dy),(int)(y1 + (d/2)*dx));
		return ret;
	}

}
