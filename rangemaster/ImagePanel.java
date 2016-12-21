
package accufiresystems.rangemaster;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

@SuppressWarnings("unused")
public class ImagePanel extends JPanel {


	private static final long serialVersionUID = 1L;
	private BufferedImage image;

	private boolean calibrating = false;
	private Point calStart;
	private Point calEnd;
	private Point calCenter;

	private boolean selectingTarget = false;
	private Point targetStart;
	private Point targetEnd;

	private int width = getWidth();
	private int height = getHeight();

	private final Color[] compColors = {Color.RED, Color.BLUE, Color.GREEN, Color.DARK_GRAY, Color.MAGENTA, Color.ORANGE};
	private int shots = 5;
	private int players = 2;

	private static final int SOLO_PRACTICE = 0;
	private static final int COMPETITION = 1;
	private static final int AREA_SHOOTING = 2;
	private static final int REACTION_TIMING = 3;
	private int selectedMode = SOLO_PRACTICE;

	public void setCompSettings(final int shots, final int players) {
		this.shots = shots;
		this.players = players;
	}

	public void setMode(final int mode) {
		selectedMode = mode;
	}

	public int getMode() {
		return selectedMode;
	}

	public void registerTarget() {
		if (targetStart != null && targetEnd != null) {
			final int x1 = targetStart.x;
			final int y1 = targetStart.y;
			final int x2 = targetEnd.x;
			final int y2 = targetEnd.y;

			final int centerX = (x1 + x2) / 2;
			final int centerY = (y1 + y2) / 2;

			final int r = (int) PointUtils.getDist(centerX, centerY, x2, y2);

			Target.targets.add(new Target(1.0*(centerX-r)/width, 1.0*(centerY-r)/height, 1.0*r/width));

			targetStart = null;
			targetEnd = null;
		}
	}

	public boolean isSelectingTarget() {
		return selectingTarget;
	}

	public void setTargetSelect(final boolean selecting) {
		selectingTarget = selecting;
	}

	public void setTargetStart(final Point start) {
		targetStart = start;
	}

	public void setTargetEnd(final Point end) {
		targetEnd = end;
	}

	public void setTargetCenter(final Point center) {
		calCenter = center;
	}

	public Point getTargetCenter() {
		return calCenter;
	}

	public void setCalStart(final Point start) {
		calStart = start;
	}

	public void setCalEnd(final Point end) {
		calEnd = end;
	}

	public Point getCalStart() {
		return calStart;
	}

	public Point getCalEnd() {
		return calEnd;
	}


	public void startCalibrating() {
		calibrating = true;
	}

	public void stopCalibrating() {
		calibrating = false;
	}

	public boolean isCalibrating() {
		return calibrating;
	}

	public int getImageWidth() {
		return width;
	}

	public int getImageHeight() {
		return height;
	}


	public void updateImage(final BufferedImage bi) {
		image = bi;
		setDimensions();
		repaint();
	}

	private void setDimensions() {
		final int startWidth = image.getWidth();
		final int startHeight = image.getHeight();

		width = getWidth();
		height = width * startHeight / startWidth;

		if (height > getHeight()) {
			height = getHeight();
			width = height * startWidth / startHeight;
		}
	}

	@Override
	public void paint(final Graphics g) {
		super.paint(g);
		if (image != null) {
			final Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

			g2.drawImage(image, 0, 0, width, height, null);

			if (selectedMode == COMPETITION && !calibrating && calStart == null && calEnd == null) {
				g2.setFont(new Font("Arial", Font.BOLD, 14));

				g2.setColor(Color.BLACK);
				g2.drawString("Please calibrate the Rangemaster SRS before using Competition Mode.", 10, 20);

				g2.setColor(Color.WHITE);
				g2.drawString("Please calibrate the Rangemaster SRS before using Competition Mode.", 11, 21);
			}

			if (selectedMode == AREA_SHOOTING) {
				for (final Target t : Target.targets) {
					g2.setColor(Color.DARK_GRAY);
					if (t.isHit()) {
						g2.setColor(Color.YELLOW);
					}

					final int x = (int)(t.getX()*width);
					final int y = (int)(t.getY()*height);
					final int r = (int)(t.getR()*width);
					g2.drawOval(x, y, r*2, r*2);
				}

				if (selectingTarget) {
					g2.setFont(new Font("Arial", Font.BOLD, 14));

					g2.setColor(Color.BLACK);
					g2.drawString("Use the left mouse button to click and drag from one edge of a target to the other.", 10, 20);
					g2.drawString("You can delete the last target you drew with the right mouse button.", 10, 40);
					g2.drawString("When you are done, click \"Finish Selecting Targets.\"", 10, 60);

					g2.setColor(Color.WHITE);
					g2.drawString("Use the left mouse button to click and drag from one edge of a target to the other.", 11, 21);
					g2.drawString("You can delete the last target you drew with the right mouse button.", 11, 41);
					g2.drawString("When you are done, click \"Finish Selecting Targets.\"", 11, 61);

					if (targetStart != null && targetEnd != null) {
						final float dash[] = {5.0f};
						g2.setStroke(new BasicStroke(1.0f,
								BasicStroke.CAP_BUTT,
								BasicStroke.JOIN_MITER,
								10.0f, dash, 0.0f));
						g2.setColor(Color.RED);
						final int x1 = targetStart.x;
						final int y1 = targetStart.y;
						final int x2 = targetEnd.x;
						final int y2 = targetEnd.y;

						final int centerX = (x1 + x2) / 2;
						final int centerY = (y1 + y2) / 2;

						final int r = (int) PointUtils.getDist(centerX, centerY, x2, y2);

						g2.drawOval(centerX - r, centerY - r, r*2, r*2);

						g2.setStroke(new BasicStroke());
					}
				}
			}


			g2.setFont(new Font("Arial", Font.PLAIN, 8));
			final FontMetrics metrics = g2.getFontMetrics();
			int totalX = 0;
			int totalY = 0;
			final int totalShots = Shot.shots.size();



			for (int i = 0; i < totalShots; i++) {
				if (selectedMode == COMPETITION && i < shots*players) {
					g2.setColor(compColors[(i/shots)%compColors.length]);
				} else {
					g2.setColor(Color.RED);
				}

				final int x = (int)(Shot.shots.get(i).getX()*width);
				final int y = (int)(Shot.shots.get(i).getY()*height);
				totalX += x;
				totalY += y;
				g2.fillOval(x - 5, y - 5, 10, 10);
				g2.setColor(Color.BLACK);
				final String textNum = (i+1)+"";
				g2.drawString(textNum, x-metrics.stringWidth(textNum)/2, y+3);
			}

			if (totalShots > 0) {
				if (selectedMode != COMPETITION) {
					g2.setColor(Color.BLUE);
					final int avgX = totalX / totalShots;
					final int avgY = totalY / totalShots;

					g2.drawLine(avgX - 3, avgY, avgX + 3, avgY);
					g2.drawLine(avgX, avgY - 3, avgX, avgY + 3);
				}
			}

			if (calibrating) {
				g2.setFont(new Font("Arial", Font.BOLD, 14));

				g2.setColor(Color.BLACK);
				g2.drawString("Use the left mouse button to click and drag from one edge of the 10-inch bar to the other.", 10, 20);
				g2.drawString("Use the right mouse button to click the center of the target.", 10, 40);
				g2.drawString("When you are done, click \"Finish Calibration.\"", 10, 60);

				g2.setColor(Color.WHITE);
				g2.drawString("Use the left mouse button to click and drag from one edge of the 10-inch bar to the other.", 11, 21);
				g2.drawString("Use the right mouse button to click the center of the target.", 11, 41);
				g2.drawString("When you are done, click \"Finish Calibration.\"", 11, 61);

				g2.setColor(Color.RED);

				if (calCenter != null) {
					final int x = calCenter.x;
					final int y = calCenter.y;
					g2.drawLine(x-3, y-3, x+3, y+3);
					g2.drawLine(x-3, y+3, x+3, y-3);
				}

				if (calStart != null && calEnd != null) {
					final float dash[] = {5.0f};
					g2.setStroke(new BasicStroke(1.0f,
							BasicStroke.CAP_BUTT,
							BasicStroke.JOIN_MITER,
							10.0f, dash, 0.0f));

					final double x1 = calStart.x;
					final double y1 = calStart.y;
					final double x2 = calEnd.x;
					final double y2 = calEnd.y;
					final double d = 15;

					final Point left[] = PointUtils.getPerpLine(x1, y1, x2, y2, d);
					final Point right[] = PointUtils.getPerpLine(x2, y2, x1, y1, d);

					g2.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
					g2.drawLine(left[0].x, left[0].y, left[1].x, left[1].y);
					g2.drawLine(right[0].x, right[0].y, right[1].x, right[1].y);
				}
			}
		}
	}

}