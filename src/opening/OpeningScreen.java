package opening;

import candy_rush.ScreenX;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Candy Rush splash screen.
 * Borderless, transparent, rounded window with floating candy particles,
 * a staggered title reveal, an animated progress bar and a fade in/out.
 */
public class OpeningScreen extends JFrame
{
    private static final int W = 1080;
    private static final int H = 650;

    public OpeningScreen()
    {
        setTitle("Candy Rush");
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(W, H);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);

        SplashPanel panel = new SplashPanel(() -> {
            dispose();
            SwingUtilities.invokeLater(ScreenX::new);
        });
        setContentPane(panel);

        setVisible(true);
        panel.start();
    }
    //  Splash panel (all the drawing and animation lives here)
    private static class SplashPanel extends JPanel
    {
        // Layout
        private static final int PAD = 24;
        private static final float RADIUS = 28f;
        // Timeline (milliseconds)
        private static final long FADE_IN = 450;
        private static final long LOAD_START = 700;
        private static final long LOAD_END = 3300;
        private static final long FADE_OUT_START = 3500;
        private static final long END = 4000;
        private static final String TITLE = "CANDY RUSH";
        private static final String SUBTITLE = "S W E E T   C H A O S   A W A I T S";
        private static final String[] STATUS = {
                "Preparing the candy factory",
                "Mixing sweet ingredients",
                "Polishing the sprinkles",
                "Loading levels",
                "Ready to rush"
        };

        private static final Color[] PALETTE = {
                new Color(0xFF5FA2), new Color(0xFFB347), new Color(0x7CF5FF),
                new Color(0xB28DFF), new Color(0x8CFF98), new Color(0xFFE66D)
        };

        private final Runnable onFinish;
        private final List<Candy> candies = new ArrayList<>();
        private final Random rng = new Random();
        private javax.swing.Timer timer;
        private long startTime;
        private long lastFrame;

        SplashPanel(Runnable onFinish)
        {
            this.onFinish = onFinish;
            setOpaque(false);

            for (int i = 0; i < 28; i++)
            {
                candies.add(new Candy(true));
            }
        }

        void start()
        {
            startTime = System.currentTimeMillis();
            lastFrame = startTime;

            timer = new javax.swing.Timer(16, e -> {
                long now = System.currentTimeMillis();
                float dt = (now - lastFrame) / 1000f;
                lastFrame = now;

                for (Candy c : candies)
                {
                    c.update(dt);
                }

                repaint();

                if (now - startTime >= END)
                {
                    timer.stop();
                    onFinish.run();
                }
            });
            timer.start();
        }

        //Easing helpers
        private static float clamp01(float v)
        {
            return Math.max(0f, Math.min(1f, v));
        }

        private static float easeOutCubic(float t)
        {
            float p = 1f - t;
            return 1f - p * p * p;
        }

        private static float smoothStep(float t)
        {
            return t * t * (3f - 2f * t);
        }

        // Painting
        @Override
        protected void paintComponent(Graphics g0)
        {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            long t = System.currentTimeMillis() - startTime;

            // Master fade in / out
            float master = clamp01(t / (float) FADE_IN);
            if (t > FADE_OUT_START)
            {
                master *= 1f - clamp01((t - FADE_OUT_START) / (float) (END - FADE_OUT_START));
            }
            g.setComposite(AlphaComposite.SrcOver.derive(master));

            int cardW = getWidth() - PAD * 2;
            int cardH = getHeight() - PAD * 2;
            RoundRectangle2D card = new RoundRectangle2D.Float(PAD, PAD, cardW, cardH, RADIUS, RADIUS);

            paintShadow(g, cardW, cardH);

            // Background gradient
            g.setPaint(new GradientPaint(PAD, PAD, new Color(0x1A0F3C),
                    PAD + cardW, PAD + cardH, new Color(0x45124F)));
            g.fill(card);

            // Everything inside the card is clipped to its rounded shape
            Shape oldClip = g.getClip();
            g.setClip(card);

            paintGlow(g, t);
            for (Candy c : candies)
            {
                c.paint(g, cardW, cardH);
            }
            paintVignette(g, cardW, cardH);
            paintTitle(g, t);
            paintProgress(g, t);

            g.setClip(oldClip);

            // Fine border
            g.setStroke(new BasicStroke(1.5f));
            g.setPaint(new Color(255, 255, 255, 45));
            g.draw(card);

            g.dispose();
        }

        private void paintShadow(Graphics2D g, int cardW, int cardH)
        {
            for (int i = PAD; i > 0; i--)
            {
                g.setColor(new Color(0, 0, 0, 5));
                g.fillRoundRect(PAD - i, PAD - i + 6, cardW + i * 2, cardH + i * 2,(int) RADIUS + i, (int) RADIUS + i);
            }
        }

        private void paintGlow(Graphics2D g, long t)
        {
            float pulse = 1f + 0.06f * (float) Math.sin(t / 500.0);
            float radius = 260f * pulse;
            Point2D center = new Point2D.Float(getWidth() / 2f, getHeight() / 2f - 30);
            g.setPaint(new RadialGradientPaint(center, radius,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 95, 162, 85), new Color(255, 95, 162, 0)}));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private void paintVignette(Graphics2D g, int cardW, int cardH)
        {
            Point2D center = new Point2D.Float(getWidth() / 2f, getHeight() / 2f);
            g.setPaint(new RadialGradientPaint(center, Math.max(cardW, cardH) * 0.7f,
                    new float[]{0.55f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        private void paintTitle(Graphics2D g, long t)
        {
            Font font = new Font("Segoe UI", Font.BOLD, 62);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int tracking = 8;
            int total = 0;

            for (char c : TITLE.toCharArray())
            {
                total += fm.charWidth(c) + tracking;
            }
            total -= tracking;

            int startX = (getWidth() - total) / 2;
            int baseY = getHeight() / 2 - 8;

            GradientPaint textPaint = new GradientPaint(startX, 0, new Color(0xFF5FA2), startX + total, 0, new Color(0xFFB347));
            Composite original = g.getComposite();

            int x = startX;
            for (int i = 0; i < TITLE.length(); i++)
            {
                char c = TITLE.charAt(i);
                float local = clamp01((t - (350 + i * 55L)) / 450f);
                float e = easeOutCubic(local);

                if (e > 0f)
                {
                    float offsetY = (1f - e) * 26f;
                    g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, ((AlphaComposite) original).getAlpha() * e)));

                    // Soft glow behind each letter
                    g.setColor(new Color(255, 95, 162, 60));
                    g.drawString(String.valueOf(c), x + 2, baseY + offsetY + 3);

                    g.setPaint(textPaint);
                    g.drawString(String.valueOf(c), x, baseY + offsetY);
                }
                x += fm.charWidth(c) + tracking;
            }
            g.setComposite(original);

            // Accent line growing from the centre
            float lineT = easeOutCubic(clamp01((t - 1000) / 600f));
            int lineW = (int) (140 * lineT);
            int lineY = baseY + 22;
            if (lineW > 0)
            {
                g.setPaint(new GradientPaint(getWidth() / 2f - 70, 0, new Color(0xFF5FA2),
                        getWidth() / 2f + 70, 0, new Color(0x7CF5FF)));
                g.fillRoundRect(getWidth() / 2 - lineW / 2, lineY, lineW, 3, 3, 3);
            }

            // Subtitle
            float subT = clamp01((t - 1200) / 600f);
            if (subT > 0f)
            {
                g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, ((AlphaComposite) original).getAlpha() * subT)));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g.setColor(new Color(255, 255, 255, 170));
                FontMetrics sfm = g.getFontMetrics();
                g.drawString(SUBTITLE, (getWidth() - sfm.stringWidth(SUBTITLE)) / 2, lineY + 34 - (int) ((1f - subT) * -8));
                g.setComposite(original);
            }
        }

        private void paintProgress(Graphics2D g, long t)
        {
            float raw = clamp01((t - LOAD_START) / (float) (LOAD_END - LOAD_START));
            float p = smoothStep(raw);

            int barX = PAD + 60;
            int barW = getWidth() - (PAD + 60) * 2;
            int barY = getHeight() - PAD - 62;
            int barH = 5;

            // Track
            g.setColor(new Color(255, 255, 255, 30));
            g.fillRoundRect(barX, barY, barW, barH, barH, barH);

            // Fill
            int fillW = (int) (barW * p);
            if (fillW > 0)
            {
                g.setPaint(new GradientPaint(barX, 0, new Color(0xFF5FA2),
                        barX + barW, 0, new Color(0xFFB347)));
                g.fillRoundRect(barX, barY, Math.max(fillW, barH), barH, barH, barH);

                // Glow at the leading edge
                float cx = barX + fillW;
                float cy = barY + barH / 2f;
                g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), 18f,
                        new float[]{0f, 1f},
                        new Color[]{new Color(255, 200, 120, 170),
                        new Color(255, 200, 120, 0)}));
                g.fill(new Ellipse2D.Float(cx - 18, cy - 18, 36, 36));
            }

            // Status text and percentage
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            int idx = Math.min(STATUS.length - 1, (int) (p * STATUS.length));
            String status = raw > 0f ? STATUS[idx] + "..." : "Starting up...";
            String pct = (int) (p * 100) + "%";

            g.setColor(new Color(255, 255, 255, 150));
            g.drawString(status, barX, barY + 24);
            g.setColor(new Color(255, 255, 255, 210));
            g.drawString(pct, barX + barW - fm.stringWidth(pct), barY + 24);
        }

        //  Floating candy particle
        private class Candy
        {
            float x, y, size, speed, rot, rotSpeed, phase, swayAmp, alpha;
            int type;
            Color color;

            Candy(boolean randomY)
            {
                reset(randomY);
            }

            void reset(boolean randomY)
            {
                size = 14 + rng.nextFloat() * 32;
                x = PAD + rng.nextFloat() * (W - PAD * 2);
                y = randomY ? PAD + rng.nextFloat() * (H - PAD * 2) : H + size;
                speed = 12 + size * 0.8f + rng.nextFloat() * 10;
                rot = rng.nextFloat() * 360f;
                rotSpeed = (rng.nextFloat() - 0.5f) * 60f;
                phase = rng.nextFloat() * 6.28f;
                swayAmp = 6 + rng.nextFloat() * 10;
                alpha = 0.22f + 0.45f * (size / 46f);
                type = rng.nextInt(5);
                color = PALETTE[rng.nextInt(PALETTE.length)];
            }

            void update(float dt)
            {
                y -= speed * dt;
                rot += rotSpeed * dt;
                phase += dt * 1.2f;
                if (y < -size * 2)
                {
                    reset(false);
                }
            }

            void paint(Graphics2D g, int cardW, int cardH)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                Composite base = g2.getComposite();
                float baseAlpha = base instanceof AlphaComposite ? ((AlphaComposite) base).getAlpha() : 1f;
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, baseAlpha * alpha)));

                g2.translate(x + Math.sin(phase) * swayAmp, y);
                g2.rotate(Math.toRadians(rot));
                g2.setColor(color);

                float s = size;
                switch (type)
                {
                    case 0: // Round candy with highlight
                        g2.fill(new Ellipse2D.Float(-s / 2, -s / 2, s, s));
                        g2.setColor(new Color(255, 255, 255, 140));
                        g2.fill(new Ellipse2D.Float(-s * 0.28f, -s * 0.34f, s * 0.3f, s * 0.2f));
                        break;

                    case 1: // Wrapped candy
                    {
                        float bw = s;
                        float bh = s * 0.62f;
                        g2.fill(new Ellipse2D.Float(-bw / 2, -bh / 2, bw, bh));

                        Path2D wrap = new Path2D.Float();
                        wrap.moveTo(-bw / 2, 0);
                        wrap.lineTo(-bw / 2 - s * 0.45f, -s * 0.3f);
                        wrap.lineTo(-bw / 2 - s * 0.45f, s * 0.3f);
                        wrap.closePath();
                        wrap.moveTo(bw / 2, 0);
                        wrap.lineTo(bw / 2 + s * 0.45f, -s * 0.3f);
                        wrap.lineTo(bw / 2 + s * 0.45f, s * 0.3f);
                        wrap.closePath();
                        g2.fill(wrap);

                        g2.setColor(new Color(255, 255, 255, 110));
                        g2.setStroke(new BasicStroke(Math.max(1.5f, s * 0.06f)));
                        g2.draw(new Line2D.Float(-bw * 0.15f, -bh / 2, bw * 0.05f, bh / 2));
                        break;
                    }

                    case 2: // Gummy square
                        g2.fill(new RoundRectangle2D.Float(-s / 2, -s / 2, s, s, s * 0.35f, s * 0.35f));
                        g2.setColor(new Color(255, 255, 255, 90));
                        g2.fill(new RoundRectangle2D.Float(-s * 0.35f, -s * 0.35f, s * 0.3f, s * 0.14f, 6, 6));
                        break;

                    case 3: // Ring
                        g2.setStroke(new BasicStroke(Math.max(3f, s * 0.2f)));
                        g2.draw(new Ellipse2D.Float(-s / 2, -s / 2, s, s));
                        break;

                    default: // Lollipop
                        g2.setStroke(new BasicStroke(Math.max(2f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.setColor(new Color(255, 255, 255, 160));
                        g2.draw(new Line2D.Float(0, 0, 0, s * 0.95f));
                        g2.setColor(color);
                        g2.fill(new Ellipse2D.Float(-s / 2, -s / 2, s, s));
                        g2.setColor(new Color(255, 255, 255, 140));
                        g2.setStroke(new BasicStroke(Math.max(1.5f, s * 0.07f)));
                        g2.draw(new Arc2D.Float(-s * 0.3f, -s * 0.3f, s * 0.6f, s * 0.6f, 20, 250, Arc2D.OPEN));
                        break;
                }
                g2.dispose();
            }
        }
    }
}
