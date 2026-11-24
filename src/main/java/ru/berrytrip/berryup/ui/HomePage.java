package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Prism;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Первая вкладка: сборка, ссылки на сайт и Telegram, короткая справка. */
public class HomePage extends JPanel {

    private final MainFrame host;
    private final JPanel warning;
    private final JLabel warningText;
    private final Widgets.BerryButton install;
    private final Widgets.BerryButton pickLauncher;
    private final JLabel infoBuild;
    private final JLabel infoServer;
    private final JLabel infoVersion;

    public HomePage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel hero = new JPanel(new BorderLayout(14, 0));
        hero.setOpaque(false);
        hero.setAlignmentX(LEFT_ALIGNMENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        hero.add(new Widgets.Logo(56), BorderLayout.WEST);
        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        JLabel title = Widgets.label(Cfg.BUILD, Palette.semi(22f), Palette.FG);
        title.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(title);
        heroText.add(Box.createVerticalStrut(3));
        JLabel sub = Widgets.label("BerryUp " + Cfg.VERSION + "  ·  " + Cfg.LOADER + "  ·  Minecraft " + Cfg.MC_VERSION,
                Palette.reg(8.8f), Palette.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(sub);
        hero.add(heroText, BorderLayout.CENTER);
        add(hero);
        add(Box.createVerticalStrut(12));

        JLabel intro = Widgets.label(
                "BerryTrip — выживание, приключения и друзья. Запуск и выбор сервера слева, настройки клиента — на вкладке «Настройки».",
                Palette.reg(9.5f), Palette.MUTED);
        intro.setAlignmentX(LEFT_ALIGNMENT);
        add(intro);
        add(Box.createVerticalStrut(14));

        // предупреждение, если сборки ещё нет
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
        add(Box.createVerticalStrut(6));

        JLabel hint = Widgets.label("Ссылки откроются в браузере.", Palette.reg(8f), Palette.DIM);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        add(hint);
        add(Box.createVerticalStrut(14));

        Widgets.Card info = new Widgets.Card();
        info.setAlignmentX(LEFT_ALIGNMENT);
        info.setLayout(new GridBagLayout());
        info.pad(12, 16, 12, 16);
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
        add(Box.createVerticalGlue());

        refresh();
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
        revalidate();
        repaint();
    }
}
