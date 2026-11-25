package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Auth;
import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Install;
import ru.berrytrip.berryup.Log;
import ru.berrytrip.berryup.Ping;
import ru.berrytrip.berryup.Prism;
import ru.berrytrip.berryup.Store;
import ru.berrytrip.berryup.Sync;
import ru.berrytrip.berryup.Tweaks;
import ru.berrytrip.berryup.UpdateCheck;
import ru.berrytrip.berryup.Updater;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/** Главное окно BerryUp: слева запуск и сервер, справа вкладки. */
public class MainFrame extends JFrame {

    private static final String[] PAGE_NAMES = {"home", "account", "settings", "diag", "about"};
    private static final String[] PAGE_TITLES = {"Главное", "Аккаунт", "Настройки", "Диагностика", "О программе"};
    private static final String[] PAGE_SUBS = {
            "Сборка, сервер и быстрый запуск — всё на одном экране.",
            "Вход через Telegram, скин и плащ QuickSkin.",
            "Моды, графика, память и журнал лаунчера.",
            "Что найдено на компьютере и хватает ли места.",
            "Версия BerryUp, обновления и ссылки."
    };

    private Prism prism;

    private final Widgets.Segmented nav;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final java.util.Map<String, javax.swing.JScrollPane> pageScrolls =
            new java.util.HashMap<String, javax.swing.JScrollPane>();
    private final HomePage home;
    private final AccountPage account;
    private final SettingsPage settings;
    private final DiagPage diag;
    private final AboutPage about;

    private final JLabel stateLabel;
    private final JLabel pageTitle;
    private final JLabel pageSub;
    private final JLabel serverStatus;
    private final Widgets.Dot serverDot = new Widgets.Dot();
    private final JTextArea serverMotd;
    private final JComboBox<String> serverBox;
    private final Widgets.BerryButton playButton;
    private final Widgets.BerryButton connectButton;
    private final Widgets.BerryButton updateButton;
    private final Widgets.BerryButton folderButton;
    private final Banner banner;
    private boolean pingRunning;
    private boolean modsSyncing;

    public MainFrame() {
        super("BerryUp " + Cfg.VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Store.load();
        Tweaks.load();
        // Окно не должно вылезать за экран: на ноутбуке с масштабом 125–150%
        // логическое разрешение меньше прежних 1060×700, и низ обрезался.
        java.awt.Rectangle usable = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        int minWidth = Math.min(1020, Math.max(720, usable.width - 60));
        int minHeight = Math.min(640, Math.max(500, usable.height - 60));
        setMinimumSize(new Dimension(minWidth, minHeight));
        int width = Math.max(minWidth, Math.min(Store.getInt("width", 1280), Math.max(minWidth, usable.width - 40)));
        int height = Math.max(minHeight, Math.min(Store.getInt("height", 840), Math.max(minHeight, usable.height - 40)));
        setSize(width, height);
        setLocationRelativeTo(null);
        try {
            java.awt.Image icon = javax.imageio.ImageIO.read(MainFrame.class.getResourceAsStream("/berry.png"));
            if (icon != null) setIconImage(icon);
        } catch (Exception ignored) {
        }
        prism = Prism.find();
        // «Запомнить меня» выключено — прошлый токен не переиспользуем
        if (prism != null && prism.hasInstance() && !Store.remember()) {
            Auth.logout(prism);
        }

        Widgets.Backdrop root = new Widgets.Backdrop(new BorderLayout(0, 0));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        // ---------------------------------------------------------- боковая панель
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        brand.add(new Widgets.Logo(48), BorderLayout.WEST);
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.add(Widgets.label(Cfg.BUILD, Palette.semi(19f), Palette.FG));
        brandText.add(Box.createVerticalStrut(1));
        JLabel brandSub = Widgets.label("Minecraft " + Cfg.MC_VERSION,
                Palette.reg(8.6f), Palette.MUTED);
        brandSub.setAlignmentX(LEFT_ALIGNMENT);
        brandText.add(brandSub);
        brandText.add(Box.createVerticalStrut(6));
        JPanel brandChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        brandChips.setOpaque(false);
        brandChips.add(new Widgets.Chip("BerryUp " + Cfg.VERSION, Palette.ACCENT));
        brandText.add(brandChips);
        brand.add(brandText, BorderLayout.CENTER);
        side.add(brand);
        side.add(Box.createVerticalStrut(16));

        playButton = new Widgets.BerryButton("Играть", Widgets.BerryButton.Kind.PRIMARY);
        playButton.setAlignmentX(LEFT_ALIGNMENT);
        playButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        playButton.setFont(Palette.semi(12.5f));
        playButton.addActionListener(e -> play(false));
        side.add(playButton);
        side.add(Box.createVerticalStrut(8));

        connectButton = new Widgets.BerryButton("Играть с подключением к серверу", Widgets.BerryButton.Kind.SECONDARY);
        connectButton.setAlignmentX(LEFT_ALIGNMENT);
        connectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        connectButton.addActionListener(e -> play(true));
        side.add(connectButton);
        side.add(Box.createVerticalStrut(14));

        // ---------------------------------------------------------- карточка сервера
        Widgets.Card serverCard = new Widgets.Card();
        serverCard.setAlignmentX(LEFT_ALIGNMENT);
        serverCard.setLayout(new BoxLayout(serverCard, BoxLayout.Y_AXIS));
        serverCard.pad(15, 16, 15, 16);
        serverCard.tint(Palette.ACCENT);
        serverCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        serverCard.add(Widgets.caption("СЕРВЕР", Palette.SURFACE));
        serverCard.add(Box.createVerticalStrut(8));

        serverBox = new JComboBox<String>(serverNames());
        serverBox.setSelectedIndex(Store.serverIndex());
        serverBox.setFont(Palette.reg(9.5f));
        serverBox.setBackground(Palette.SURFACE);
        serverBox.setForeground(Palette.FG);
        serverBox.setFocusable(false);
        serverBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        serverBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.LINE, 1, true),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        serverBox.setAlignmentX(LEFT_ALIGNMENT);
        serverBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                label.setBackground(selected ? Palette.HOVER : Palette.SURFACE_ALT);
                label.setForeground(Palette.FG);
                return label;
            }
        });
        serverBox.addActionListener(e -> {
            if (serverBox.getSelectedIndex() < 0) return;
            Store.setServerIndex(serverBox.getSelectedIndex());
            Store.save();
            pingServer();
        });
        serverCard.add(serverBox);
        serverCard.add(Box.createVerticalStrut(10));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(LEFT_ALIGNMENT);
        statusRow.add(serverDot);
        serverStatus = Widgets.label("проверяю…", Palette.reg(8.5f), Palette.MUTED);
        statusRow.add(serverStatus);
        serverCard.add(statusRow);
        serverCard.add(Box.createVerticalStrut(6));

        serverMotd = Widgets.paragraph(selectedServer().endpoint() + "\nпроверяю…",
                Palette.reg(8.5f), Palette.DIM);
        serverMotd.setAlignmentX(LEFT_ALIGNMENT);
        serverMotd.setRows(3);
        serverCard.add(serverMotd);
        serverCard.add(Box.createVerticalGlue());

        Widgets.BerryButton copy = new Widgets.BerryButton("Скопировать адрес", Widgets.BerryButton.Kind.GHOST);
        copy.setAlignmentX(LEFT_ALIGNMENT);
        copy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        copy.addActionListener(e -> {
            Cfg.Server server = selectedServer();
            if (server != null && ru.berrytrip.berryup.Sys.copy(server.endpoint())) {
                Log.ok("Адрес скопирован: " + server.endpoint());
                setState("Адрес скопирован", Palette.OK);
            }
        });
        serverCard.add(copy);

        side.add(serverCard);
        side.add(Box.createVerticalStrut(12));

        JPanel quick = new JPanel(new GridLayout(1, 2, 8, 0));
        quick.setOpaque(false);
        quick.setAlignmentX(LEFT_ALIGNMENT);
        quick.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        updateButton = new Widgets.BerryButton("Обновить сборку", Widgets.BerryButton.Kind.SECONDARY);
        updateButton.addActionListener(e -> updatePack());
        folderButton = new Widgets.BerryButton("Папка сборки", Widgets.BerryButton.Kind.SECONDARY);
        folderButton.addActionListener(e -> openInstanceFolder());
        quick.add(updateButton);
        quick.add(folderButton);
        side.add(quick);

        side.add(Box.createVerticalGlue());
        JLabel sideFoot = Widgets.label("BerryUp " + Cfg.VERSION + "  ·  %APPDATA%\\BerryUp",
                Palette.reg(7.8f), Palette.DIM);
        sideFoot.setAlignmentX(LEFT_ALIGNMENT);
        side.add(sideFoot);

        // Колонка сужается вместе с окном, а на низком экране прокручивается.
        // Ширину задаём обёртке, высоту берём настоящую — иначе BoxLayout
        // сжимает карточки до нуля, и колонка выглядит пустой.
        final int[] sideWidth = {336};
        Widgets.Card sideCard = new Widgets.Card(new BorderLayout());
        sideCard.pad(18, 20, 14, 20);
        sideCard.add(Widgets.scroll(side), BorderLayout.CENTER);
        JPanel sideHolder = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(sideWidth[0], Math.max(10, size.height));
            }
        };
        sideHolder.setOpaque(false);
        sideHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        sideHolder.add(sideCard, BorderLayout.CENTER);
        root.add(sideHolder, BorderLayout.WEST);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                int target = Math.max(300, Math.min(336, getWidth() / 3));
                if (sideWidth[0] != target) {
                    sideWidth[0] = target;
                    sideHolder.revalidate();
                }
            }
        });

        // ---------------------------------------------------------- правая часть
        nav = new Widgets.Segmented("Главное", "Аккаунт", "Настройки", "Диагностика", "О программе");
        nav.onSelect(this::showPage);
        nav.setAlignmentX(LEFT_ALIGNMENT);

        banner = new Banner();

        // GridBag вместо BorderLayout: при узком окне кнопки навигации
        // переносятся на вторую строку, а не уезжают за край.
        JPanel header = new JPanel(new java.awt.GridBagLayout());
        header.setOpaque(false);
        java.awt.GridBagConstraints headNav = new java.awt.GridBagConstraints();
        headNav.gridx = 0;
        headNav.weightx = 1;
        headNav.fill = java.awt.GridBagConstraints.HORIZONTAL;
        headNav.anchor = java.awt.GridBagConstraints.WEST;
        header.add(nav, headNav);
        Widgets.BerryButton site = new Widgets.BerryButton("Сайт сборки", Widgets.BerryButton.Kind.GHOST);
        site.addActionListener(e -> ru.berrytrip.berryup.Sys.openUrl(Cfg.SITE));
        java.awt.GridBagConstraints headSite = new java.awt.GridBagConstraints();
        headSite.gridx = 1;
        headSite.weightx = 0;
        headSite.anchor = java.awt.GridBagConstraints.EAST;
        headSite.insets = new java.awt.Insets(0, 8, 0, 0);
        header.add(site, headSite);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setBorder(BorderFactory.createEmptyBorder(18, 2, 2, 0));
        pageTitle = Widgets.label(PAGE_TITLES[0], Palette.semi(24f), Palette.FG);
        pageTitle.setAlignmentX(LEFT_ALIGNMENT);
        pageSub = Widgets.label(PAGE_SUBS[0], Palette.reg(9.6f), Palette.MUTED);
        pageSub.setAlignmentX(LEFT_ALIGNMENT);
        titles.add(pageTitle);
        titles.add(Box.createVerticalStrut(4));
        titles.add(pageSub);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(banner, BorderLayout.NORTH);
        top.add(header, BorderLayout.CENTER);
        top.add(titles, BorderLayout.SOUTH);

        home = new HomePage(this);
        account = new AccountPage(this);
        settings = new SettingsPage(this);
        diag = new DiagPage(this);
        about = new AboutPage(this);

        pages.setOpaque(false);
        pages.setBorder(BorderFactory.createEmptyBorder(14, 2, 0, 2));
        pageScrolls.put(PAGE_NAMES[0], Widgets.scroll(home));
        pageScrolls.put(PAGE_NAMES[1], Widgets.scroll(account));
        pageScrolls.put(PAGE_NAMES[3], Widgets.scroll(diag));
        pageScrolls.put(PAGE_NAMES[4], Widgets.scroll(about));
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            if (i == 2) pages.add(settings, PAGE_NAMES[2]); // у «Настроек» своя прокрутка
            else pages.add(pageScrolls.get(PAGE_NAMES[i]), PAGE_NAMES[i]);
        }

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 2));
        stateLabel = Widgets.label("Готово", Palette.reg(8.2f), Palette.DIM);
        bottom.add(stateLabel, BorderLayout.WEST);
        JLabel version = Widgets.label("BerryUp " + Cfg.VERSION + "  ·  журнал: %TEMP%\\BerryUp.log",
                Palette.reg(8f), Palette.DIM);
        bottom.add(version, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(pages, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        Log.addListener((text, level) -> {
            if (settings != null) settings.appendLog(text);
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                refreshAll();
                if (System.getProperty("berryup.render") != null) return;
                pingServer();
                if (Updater.pending()) {
                    banner.showStaged("Скачанное обновление применится при перезапуске");
                } else if (Store.autoUpdate()) {
                    checkUpdates(false);
                }
                Log.info("BerryUp " + Cfg.VERSION + " запущен. " + Store.describePaths());
            }

            @Override
            public void windowClosing(WindowEvent e) {
                Store.set("width", String.valueOf(getWidth()));
                Store.set("height", String.valueOf(getHeight()));
                Store.save();
            }
        });

        showPage(0);
    }

    // ------------------------------------------------------------ доступ

    public Prism prism() {
        return prism;
    }

    public HomePage home() {
        return home;
    }

    public AccountPage account() {
        return account;
    }

    public SettingsPage settings() {
        return settings;
    }

    public DiagPage diag() {
        return diag;
    }

    public AboutPage about() {
        return about;
    }

    public Cfg.Server selectedServer() {
        return Cfg.SERVERS[Math.max(0, Math.min(Cfg.SERVERS.length - 1, serverBox.getSelectedIndex()))];
    }

    private String[] serverNames() {
        String[] names = new String[Cfg.SERVERS.length];
        for (int i = 0; i < Cfg.SERVERS.length; i++) names[i] = Cfg.SERVERS[i].name;
        return names;
    }

    // ------------------------------------------------------------ действия

    public void rescan() {
        prism = Prism.find();
        refreshAll();
    }

    public void refreshAll() {
        boolean ready = prism != null && prism.hasInstance();
        playButton.setEnabled(ready);
        connectButton.setEnabled(ready);
        updateButton.setEnabled(ready);
        folderButton.setEnabled(prism != null);
        home.refresh();
        account.refresh();
        settings.refresh();
        diag.refresh();
        about.refresh();
        setState(ready ? "Сборка готова" : (prism == null ? "Prism Launcher не найден" : "Сборка не установлена"),
                ready ? Palette.OK : Palette.WARN);
    }

    public void showPage(int index) {
        int safe = Math.max(0, Math.min(4, index));
        nav.select(safe, false);
        pageLayout.show(pages, PAGE_NAMES[safe]);
        pageTitle.setText(PAGE_TITLES[safe]);
        pageSub.setText(PAGE_SUBS[safe]);
        javax.swing.JScrollPane scroller = pageScrolls.get(PAGE_NAMES[safe]);
        if (scroller != null) {
            // сфокусированное поле не должно оставлять страницу промотанной вниз
            SwingUtilities.invokeLater(() -> scroller.getVerticalScrollBar().setValue(0));
        }
        if (safe == 1) account.refresh();
        if (safe == 3) diag.refresh();
    }

    public void setTab(int index) {
        showPage(index);
    }

    public void setState(String text, Color color) {
        stateLabel.setText(text);
        stateLabel.setForeground(color);
    }

    /** Ярлыки на рабочем столе и в «Пуске» — кнопка в настройках. */
    public void makeShortcut() {
        setState("Создаю ярлыки…", Palette.WARN);
        new Thread(() -> {
            boolean ok = Install.createShortcuts();
            SwingUtilities.invokeLater(() -> setState(
                    ok ? "Ярлык BerryUp на рабочем столе и в «Пуске»" : "Ярлык не создан — смотри журнал",
                    ok ? Palette.OK : Palette.ERR));
        }, "berry-shortcut").start();
    }

    /** Переключение светлой/тёмной темы: пересобираем окно с новыми цветами. */
    public void applyTheme(boolean dark) {
        Store.setBool("theme.dark", dark);
        Store.set("width", String.valueOf(getWidth()));
        Store.set("height", String.valueOf(getHeight()));
        Store.save();
        Palette.applyTheme(dark);
        ru.berrytrip.berryup.Main.setupLook(dark);
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /** Масштаб интерфейса: сохраняем и перезапускаемся с нужным sun.java2d.uiScale. */
    public void applyUiScale(String value) {
        String label = value == null || value.isEmpty() || "auto".equals(value)
                ? "как в системе" : value.replace(',', '.');
        int answer = JOptionPane.showConfirmDialog(this,
                "Масштаб интерфейса: " + label + ".\nЧтобы применить, BerryUp перезапустится. Продолжить?",
                Cfg.APP_NAME, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            settings.refreshScale();
            return;
        }
        Store.set("ui.scale", value == null ? "" : value);
        Store.save();
        try {
            Install.restart(value);
            dispose();
            System.exit(0);
        } catch (Exception error) {
            Log.error("Масштаб не применился: " + error.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Не удалось перезапустить BerryUp:\n" + error.getMessage(),
                    Cfg.APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    public void play(boolean connect) {
        if (prism != null && prism.hasInstance() && prism.modCount() == 0 && !modsSyncing) {
            // инстанс только что установлен: сначала packwiz, иначе игра запустится без модов
            syncModsThenPlay(connect);
            return;
        }
        if (prism == null || !prism.hasInstance()) {
            setState("Сборка не установлена", Palette.WARN);
            JOptionPane.showMessageDialog(this,
                    "Сначала установи сборку на вкладке «Главное».",
                    Cfg.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!ru.berrytrip.berryup.Mods.ensure(prism)) {
            Log.error("Мод входа не установлен — сервер попросит /verify");
        }
        try {
            Sync.launch(prism, connect, serverBox.getSelectedIndex());
            setState(connect ? "Запускаю и подключаю" : "Игра запускается", Palette.OK);
            if (Store.minimizeOnPlay()) setState(JFrame.ICONIFIED);
        } catch (Exception error) {
            Log.error("Не удалось запустить игру: " + error.getMessage());
            setState("Игра не запустилась", Palette.ERR);
            JOptionPane.showMessageDialog(this, "Не удалось запустить игру:\n" + error.getMessage(),
                    Cfg.APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Докачивает моды и только потом запускает игру. */
    private void syncModsThenPlay(boolean connect) {
        modsSyncing = true;
        setState("Скачиваю моды сборки…", Palette.WARN);
        playButton.setEnabled(false);
        connectButton.setEnabled(false);
        new Thread(() -> {
            String failure = null;
            try {
                int code = Sync.updatePack(prism);
                if (code != 0) failure = "packwiz завершился с кодом " + code;
                else if (prism.modCount() == 0) failure = "моды не появились в папке сборки";
            } catch (Exception error) {
                failure = error.getMessage();
            }
            final String problem = failure;
            SwingUtilities.invokeLater(() -> {
                modsSyncing = false;
                playButton.setEnabled(true);
                connectButton.setEnabled(true);
                diag.refresh();
                if (problem == null) {
                    setState("Моды скачаны", Palette.OK);
                    play(connect);
                } else {
                    setState("Моды не скачались", Palette.ERR);
                    JOptionPane.showMessageDialog(this,
                            "Не удалось скачать моды сборки:\n" + problem
                                    + "\n\nОткрой Prism и запусти игру один раз — он скачает Java,\n"
                                    + "затем нажми «Обновить сборку» или «Играть» ещё раз.",
                            Cfg.APP_NAME, JOptionPane.WARNING_MESSAGE);
                }
            });
        }, "berry-first-sync").start();
    }

    public void updatePack() {
        if (prism == null || !prism.hasInstance()) {
            setState("Сборка не установлена", Palette.WARN);
            return;
        }
        updateButton.setEnabled(false);
        setState("Обновляю сборку…", Palette.WARN);
        new Thread(() -> {
            try {
                int code = Sync.updatePack(prism);
                SwingUtilities.invokeLater(() -> {
                    setState(code == 0 ? "Сборка обновлена" : "Обновление с ошибкой (" + code + ")",
                            code == 0 ? Palette.OK : Palette.ERR);
                    updateButton.setEnabled(true);
                    diag.refresh();
                });
            } catch (Exception error) {
                Log.error("Ошибка обновления: " + error.getMessage());
                SwingUtilities.invokeLater(() -> {
                    setState("Обновление не удалось", Palette.ERR);
                    updateButton.setEnabled(true);
                });
            }
        }, "berry-pack-update").start();
    }

    public void installInstance() {
        if (prism == null) {
            JOptionPane.showMessageDialog(this,
                    "Prism Launcher не найден. Установи его и укажи путь в настройках.",
                    Cfg.APP_NAME, JOptionPane.WARNING_MESSAGE);
            return;
        }
        setState("Устанавливаю сборку…", Palette.WARN);
        new Thread(() -> {
            try {
                Sync.install(prism, (text, bar) -> SwingUtilities.invokeLater(() -> setState(text, Palette.WARN)));
                // моды качает packwiz: без них инстанс запустится пустым
                SwingUtilities.invokeLater(() -> setState("Скачиваю моды сборки…", Palette.WARN));
                String warning = null;
                try {
                    int code = Sync.updatePack(prism);
                    if (code != 0) warning = "packwiz завершился с кодом " + code;
                } catch (Exception syncError) {
                    warning = syncError.getMessage();
                }
                final String problem = warning;
                SwingUtilities.invokeLater(() -> {
                    refreshAll();
                    if (problem == null) {
                        setState("Сборка установлена", Palette.OK);
                    } else {
                        setState("Сборка установлена, моды не скачались", Palette.WARN);
                        JOptionPane.showMessageDialog(this,
                                "Сборка распакована, но моды не скачались:\n" + problem
                                        + "\n\nЗапусти Prism один раз, чтобы он скачал Java,\n"
                                        + "затем нажми «Обновить сборку».",
                                Cfg.APP_NAME, JOptionPane.WARNING_MESSAGE);
                    }
                });
            } catch (Exception error) {
                Log.error("Установка не удалась: " + error.getMessage());
                SwingUtilities.invokeLater(() -> {
                    setState("Установка не удалась", Palette.ERR);
                    JOptionPane.showMessageDialog(this, "Не удалось установить сборку:\n" + error.getMessage(),
                            Cfg.APP_NAME, JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "berry-install").start();
    }

    public void openInstanceFolder() {
        if (prism == null) return;
        if (!prism.hasInstance()) {
            setState("Сборка не установлена", Palette.WARN);
            return;
        }
        ru.berrytrip.berryup.Sys.openFolder(prism.instanceDir());
    }

    /** Скачивает новый jar и оставляет его в очереди на замену при перезапуске. */
    public void downloadUpdate(UpdateCheck.Info info) {
        if (info == null) return;
        setState("Скачиваю обновление…", Palette.WARN);
        new Thread(() -> {
            try {
                Updater.stage(info, (received, total) -> {
                    int percent = total > 0 ? (int) (received * 100 / total) : 0;
                    SwingUtilities.invokeLater(() -> setState("Скачиваю обновление… " + percent + "%", Palette.WARN));
                });
                SwingUtilities.invokeLater(() -> {
                    setState("Обновление скачано", Palette.OK);
                    banner.showStaged("Нажми «Перезапустить» — BerryUp заменит файл сам");
                });
            } catch (Exception error) {
                Log.error("Обновление не скачалось: " + error.getMessage());
                SwingUtilities.invokeLater(() -> {
                    setState("Обновление не скачалось", Palette.ERR);
                    JOptionPane.showMessageDialog(this,
                            "Не удалось скачать обновление:\n" + error.getMessage()
                                    + "\n\nМожно скачать новый BerryUp.jar вручную: " + Cfg.JAR_URL,
                            Cfg.APP_NAME, JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "berry-update-download").start();
    }

    /**
     * Перезапуск для применения обновления: BerryUp запускает второй процесс,
     * тот дожидается закрытия окна, подменяет jar и открывает новую версию.
     */
    public void restartForUpdate() {
        try {
            Updater.apply();
            dispose();
            System.exit(0);
        } catch (Exception error) {
            Log.error("Обновление не применилось: " + error.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Не удалось применить обновление: " + error.getMessage()
                            + "\n\nСкачай новый BerryUp.jar и замени файл вручную: " + Cfg.JAR_URL,
                    Cfg.APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Выбор prismlauncher.exe / pineconemc.exe вручную — для портативных сборок. */
    public void chooseLauncher() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Где лежит prismlauncher.exe или pineconemc.exe?");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Лаунчер (*.exe)", "exe"));
        Prism current = prism();
        if (current != null) chooser.setCurrentDirectory(current.exe.getParentFile());
        else chooser.setCurrentDirectory(new File(System.getenv("ProgramFiles") != null ? System.getenv("ProgramFiles") : "C:/"));
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        Prism found = Prism.fromExe(chooser.getSelectedFile(), null);
        if (found == null) {
            JOptionPane.showMessageDialog(this, "Это не похоже на Prism Launcher или PineconeMC.",
                    Cfg.APP_NAME, JOptionPane.WARNING_MESSAGE);
            return;
        }
        Store.setExe(found.exe.getAbsolutePath());
        Store.setData(found.dataDir.getAbsolutePath());
        Store.save();
        Log.ok("Лаунчер выбран: " + found.kind + " · " + found.exe.getAbsolutePath());
        rescan();
    }

    /** Папка данных лаунчера — если exe в одном месте, а instances в другом. */
    public void chooseDataDir() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Папка данных Prism (внутри должна быть папка instances)");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        Prism current = prism();
        chooser.setCurrentDirectory(current != null ? current.dataDir : new File(Cfg.ROAMING, "PrismLauncher"));
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        File dir = chooser.getSelectedFile();
        Store.setData(dir.getAbsolutePath());
        if (Store.exe().isEmpty() && prism() != null) Store.setExe(prism().exe.getAbsolutePath());
        Store.save();
        Log.ok("Папка данных: " + dir.getAbsolutePath());
        rescan();
    }

    public void checkUpdates(boolean manual) {
        if (manual) setState("Проверяю обновления…", Palette.WARN);
        new Thread(() -> {
            UpdateCheck.Info info = UpdateCheck.check();
            SwingUtilities.invokeLater(() -> {
                if (info != null && UpdateCheck.isNewer(info)) {
                    banner.show(info);
                    setState("Доступна версия " + info.version, Palette.ACCENT);
                } else if (manual) {
                    setState(info == null ? "Проверка не удалась: " + UpdateCheck.lastError()
                            : "Обновлений нет: " + Cfg.VERSION, info == null ? Palette.ERR : Palette.OK);
                }
                about.refreshStatus(info);
            });
        }, "berry-update-check").start();
    }

    public void pingServer() {
        if (pingRunning) return;
        pingRunning = true;
        Cfg.Server server = selectedServer();
        serverDot.setColor(Palette.DIM);
        serverStatus.setText("проверяю…");
        serverStatus.setForeground(Palette.MUTED);
        new Thread(() -> {
            Ping.Status status = Ping.query(server.host, server.port, 5000);
            SwingUtilities.invokeLater(() -> {
                pingRunning = false;
                serverDot.setColor(status.ok ? Palette.OK : Palette.ERR);
                if (status.ok) {
                    serverStatus.setText("онлайн · " + status.players() + " · " + status.ping + " мс");
                    serverStatus.setForeground(Palette.MUTED);
                    String motd = status.motd.isEmpty() ? server.endpoint() : status.motd;
                    serverMotd.setText(motd + "\n" + server.endpoint());
                } else {
                    serverStatus.setText("офлайн · " + status.error);
                    serverStatus.setForeground(Palette.MUTED);
                    serverMotd.setText("Сервер " + server.endpoint() + "\n" + status.error);
                }
            });
        }, "berry-ping").start();
    }

    // ------------------------------------------------------------ баннер обновления

    private class Banner extends JPanel {
        private final JLabel title = Widgets.label("Доступно обновление", Palette.semi(10.5f), Palette.FG);
        private final JLabel notes = Widgets.label("", Palette.reg(8.5f), Palette.MUTED);
        private final Widgets.BerryButton action = new Widgets.BerryButton("Скачать обновление",
                Widgets.BerryButton.Kind.PRIMARY);
        private UpdateCheck.Info info;
        private boolean staged;

        Banner() {
            setOpaque(false);
            setLayout(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            notes.setFont(Palette.reg(8.5f));
            text.add(title);
            text.add(Box.createVerticalStrut(3));
            text.add(notes);
            add(text, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            action.addActionListener(e -> {
                if (staged) restartForUpdate();
                else downloadUpdate(info);
            });
            Widgets.BerryButton later = new Widgets.BerryButton("Позже", Widgets.BerryButton.Kind.GHOST);
            later.addActionListener(e -> setVisible(false));
            buttons.add(action);
            buttons.add(later);
            add(buttons, BorderLayout.EAST);
            setVisible(false);
        }

        void show(UpdateCheck.Info info) {
            this.info = info;
            this.staged = false;
            title.setText("Доступна версия " + info.version + "  ·  сейчас " + Cfg.VERSION);
            String text = info.notes == null || info.notes.isEmpty()
                    ? "Новая версия BerryUp.jar: скачай её прямо в лаунчере"
                    : info.notes;
            notes.setText(text);
            action.setText("Скачать обновление");
            setVisible(true);
        }

        void showStaged(String text) {
            this.staged = true;
            title.setText("Обновление готово");
            notes.setText(text);
            action.setText("Перезапустить");
            setVisible(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            Palette.fillRound(g, 0, 0, w, h, 16, Palette.mix(Palette.SURFACE, Palette.ACCENT, 0.07f));
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 16, Palette.alpha(Palette.ACCENT, 80));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Для тестов: выбрать вкладку и отрисовать её в PNG. */
    public JComponent[] tabs() {
        return new JComponent[]{home, account, settings, diag, about};
    }

    public Font heading() {
        return Palette.semi(15f);
    }
}
