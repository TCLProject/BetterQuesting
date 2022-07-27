package betterquesting.api2.client.gui.resources.lines;

import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import org.lwjgl.opengl.GL11;

public class BoxLine implements IGuiLine {
    private final short pattern;
    private final int scale;

    public BoxLine() {
        this(1, (short) 0xFFFF);
    }

    public BoxLine(int stippleScale, short stipplePattern) {
        this.pattern = stipplePattern;
        this.scale = stippleScale;
    }

    @Override
    public void drawLine(IGuiRect start, IGuiRect end, int width, IGuiColor color, float partialTick) {
        int minX = Math.min(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxX = Math.max(start.getX() + start.getWidth(), end.getX() + end.getWidth());
        int maxY = Math.max(start.getY() + start.getHeight(), end.getY() + end.getHeight());

        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_STIPPLE);
        color.applyGlColor();
        GL11.glLineWidth(width);
        GL11.glLineStipple(scale, pattern);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(minX, minY);
        GL11.glVertex2f(maxX, minY);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(minX, maxY);
        GL11.glVertex2f(maxX, maxY);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(minX, minY);
        GL11.glVertex2f(minX, maxY);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(maxX, minY);
        GL11.glVertex2f(maxX, maxY);
        GL11.glEnd();

        GL11.glLineStipple(1, (short) 0xFFFF);
        GL11.glLineWidth(1F);
        GL11.glDisable(GL11.GL_LINE_STIPPLE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);

        GL11.glPopMatrix();
    }
}
