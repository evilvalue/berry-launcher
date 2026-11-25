package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Prism;
import ru.berrytrip.berryup.Tweaks;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Первая вкладка: сборка, короткая сводка, ссылки на сайт и Telegram. */
public class HomePage extends JPanel {

    private final MainFrame host;
    private final JPanel warning;
    private final JLabel warningText;
    private final Widgets.BerryButton install;
    private final Widgets.BerryButton pickLauncher;
    private final JLabel infoBuild;
    private final JLabel infoServer;
    private final JLabel infoVersion;
    private final JLabel statMods;
    private final JLabel statMemory;
    private final JLabel heroChip;

    public HomePage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // ---------------------------------------------------------- hero-карточка
        JPanel hero = new JPanel(new BorderLayout(18, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D g = Palette.quality(graphics);
                int w = getWidth();
                int h = getHeight();
                Palette.fillRound(g, 0, 0, w, h, 20,
                        new java.awt.GradientPaint(0, 0, Palette.mix(Palette.SURFACE, Palette.ACCENT, 0.13f),
                                0, h, Palette.SURFACE));
                Widgets.glow(g, w - 30, 0, 240, Palette.ACCENT, 40);
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, 20, Palette.alpha(Palette.ACCENT, 70));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        hero.setOpaque(false);
        hero.setAlignmentX(LEFT_ALIGNMENT);
        hero.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 148));
        hero.add(new Widgets.Logo(66), BorderLayout.WEST);

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        JLabel title = Widgets.label(Cfg.BUILD, Palette.semi(25f), Palette.FG);
        title.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(2));
        JLabel sub = Widgets.label("Minecraft " + Cfg.MC_VERSION + "  ·  " + Cfg.LOADER, Palette.reg(8.8f), Palette.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(sub);
        heroText.add(Box.createVerticalStrut(8));
        JLabel intro = Widgets.label(
                "Выживание, приключения и друзья. Запуск и сервер слева, настройки клиента — во вкладке «Настройки».",
                Palette.reg(9.2f), Palette.MUTED);
        intro.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(intro);
        heroText.add(Box.createVerticalStrut(8));

        heroChip = Widgets.label("", Palette.semi(8.4f), Palette.ACCENT_HOVER);
        heroChip.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(heroChip);
        hero.add(heroText, BorderLayout.CENTER);
        add(hero);
        add(Box.createVerticalStrut(12));

        // ---------------------------------------------------------- предупреждение
        warning = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(java.awt.Graphics graphics) {
                java.awt.Graphics2D g = Palette.quality(graphics);
                int w = getWidth();
                int h = getHeight();
                Palette.fillRound(g, 0, 0, w, h, 14, Palette.mix(Palette.SURFACE, Palette.WARN, 0.10f));
                g.setStroke(new java.awt.BasicStroke(1f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, 14, Palette.alpha(Palette.WARN, 110));
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        warning.setOpaque(false);
        warning.setAlignmentX(LEFT_ALIGNMENT);
        warning.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        warning.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        warningText = Widgets.label("Сборка ещё не установлена в Prism.", Palette.reg(9f), Palette.WARN);
        warning.add(warningText, BorderLayout.CENTER);
        install = new Widgets.BerryButton("Установить сборку", Widgets.BerryButton.Kind.PRIMARY);
        install.addActionListener(e -> {
            if (host.prism() == null) {
                ru.berrytrip.berryup.Sys.openUrl(Cfg.PRISM_URL);
                return;
            }
            host.installInstance();
        });
        JPanel warningButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        warningButtons.setOpaque(false);
        pickLauncher = new Widgets.BerryButton("Указать путь…", Widgets.BerryButton.Kind.SECONDARY);
        pickLauncher.addActionListener(e -> host.chooseLauncher());
        warningButtons.add(pickLauncher);
        warningButtons.add(install);
        warning.add(warningButtons, BorderLayout.EAST);
        add(warning);
        add(Box.createVerticalStrut(12));

        // ---------------------------------------------------------- сводка
        JPanel stats = new JPanel(new GridLayout(1, 3, 10, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        statMods = statCard(stats, "МОДОВ", Palette.ACCENT_HOVER);
        statMemory = statCard(stats, "ВЫДЕЛЕНО ПАМЯТИ", Palette.OK);
        statCard(stats, "ВЕРСИЯ BERRYUP", Palette.FG).setText("v" + Cfg.VERSION);
        add(stats);
        add(Box.createVerticalStrut(16));

        // ---------------------------------------------------------- ссылки
        JLabel linksCaption = Widgets.label("ГДЕ СЛЕДИТЬ ЗА СБОРКОЙ", Palette.semi(8f), Palette.DIM);
        linksCaption.setAlignmentX(LEFT_ALIGNMENT);
        add(linksCaption);
        add(Box.createVerticalStrut(8));

        Widgets.LinkCard site = new Widgets.LinkCard("Сайт сборки", "evilvalue.github.io/berry", Cfg.SITE, Widgets.Glyph.SITE);
        site.setAlignmentX(LEFT_ALIGNMENT);
        site.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        add(site);
        add(Box.createVerticalStrut(8));

        Widgets.LinkCard telegram = new Widgets.LinkCard("Telegram-канал", "@killberries", Cfg.TELEGRAM, Widgets.Glyph.TELEGRAM);
        telegram.setAlignmentX(LEFT_ALIGNMENT);
        telegram.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        add(telegram);
        add(Box.createVerticalStrut(14));

        Widgets.Card info = new Widgets.Card();
        info.setAlignmentX(LEFT_ALIGNMENT);
        info.setLayout(new GridBagLayout());
        info.pad(14, 16, 14, 16);
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 6, 0);
        info.add(Widgets.label("КОРОТКО О СБОРКЕ", Palette.semi(8f), Palette.DIM), gc);

        infoBuild = Widgets.label(Cfg.BUILD + "  ·  " + Cfg.LOADER + "  ·  Minecraft " + Cfg.MC_VERSION,
                Palette.reg(9f), Palette.FG);
        infoServer = Widgets.label(Cfg.SERVER_HOST + ":" + Cfg.SERVER_PORT, Palette.reg(9f), Palette.FG);
        infoVersion = Widgets.label("BerryUp " + Cfg.VERSION, Palette.reg(9f), Palette.FG);

        addRow(info, gc, 1, "Сборка", infoBuild);
        addRow(info, gc, 2, "Сервер", infoServer);
        addRow(info, gc, 3, "Версия", infoVersion);
        add(info);
        add(Box.createVerticalStrut(10));

        JLabel hint = Widgets.label("Ссылки откроются в браузере. Журнал и диагностика — в соседних вкладках.",
                Palette.reg(8f), Palette.DIM);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        add(hint);
        add(Box.createVerticalGlue());

        refresh();
    }

    /** Маленькая карточка-цифра для сводки. */
    private JLabel statCard(JPanel parent, String caption, java.awt.Color valueColor) {
        Widgets.Card card = new Widgets.Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.pad(12, 14, 12, 14);
        JLabel value = Widgets.label("—", Palette.semi(15f), valueColor);
        value.setAlignmentX(LEFT_ALIGNMENT);
        card.add(value);
        card.add(Box.createVerticalStrut(3));
        JLabel text = Widgets.label(caption, Palette.semi(7.4f), Palette.DIM);
        text.setAlignmentX(LEFT_ALIGNMENT);
        card.add(text);
        parent.add(card);
        return value;
    }

    private void addRow(JPanel card, GridBagConstraints gc, int row, String key, JLabel value) {
        gc.gridy = row;
        gc.gridwidth = 1;
        gc.insets = new Insets(2, 0, 2, 12);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        card.add(Widgets.label(key, Palette.reg(8.5f), Palette.MUTED), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        card.add(value, gc);
        gc.gridx = 0;
    }

    public void refresh() {
        Prism prism = host.prism();
        boolean hasLauncher = prism != null;
        boolean installed = hasLauncher && prism.hasInstance();
        warning.setVisible(!installed);
        if (!installed) {
            warningText.setText(hasLauncher
                    ? "Сборка BerryTrip не установлена в " + prism.kind + "."
                    : "Prism Launcher не найден — BerryUp работает поверх него.");
            install.setEnabled(true);
            install.setText(hasLauncher ? "Установить сборку" : "Скачать Prism");
            pickLauncher.setVisible(!hasLauncher);
        }

        int mods = prism != null && prism.hasInstance() ? prism.modCount() : 0;
        statMods.setText(mods > 0 ? String.valueOf(mods) : "—");
        statMemory.setText(Tweaks.effectiveMemory() + " ГБ");
        heroChip.setText(installed
                ? "модов: " + mods + "  ·  сборка готова к игре"
                : "Сборка ставится в один клик  ·  BerryUp " + Cfg.VERSION);

        revalidate();
        repaint();
    }
}
