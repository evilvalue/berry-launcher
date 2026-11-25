package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Auth;
import ru.berrytrip.berryup.Cfg;
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

    private Prism prism;

    private final Widgets.Segmented nav;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final HomePage home;
    private final AccountPage account;
    private final SettingsPage settings;
    private final DiagPage diag;
    private final AboutPage about;

    private final JLabel stateLabel;
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
        setMinimumSize(new Dimension(1060, 700));
        Store.load();
        Tweaks.load();
        setSize(Math.max(1060, Store.getInt("width", 1200)), Math.max(700, Store.getInt("height", 800)));
        setLocationRelativeTo(null);
        try {
            java.awt.Image icon = javax.imageio.ImageIO.read(MainFrame.class.getResourceAsStream("/berry.png"));
            if (icon != null) setIconImage(icon);
        } catch (Exception ignored) {
        }
        prism = Prism.find();

        Widgets.Backdrop root = new Widgets.Backdrop(new BorderLayout(0, 0));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 14, 18));
        setContentPane(root);

        // ---------------------------------------------------------- боковая панель
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(360, 100));
        side.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));

        Widgets.Card hero = new Widgets.Card();
        hero.setAlignmentX(LEFT_ALIGNMENT);
        hero.setLayout(new BorderLayout(13, 0));
        hero.pad(14, 16, 14, 16);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        hero.add(new Widgets.Logo(54), BorderLayout.WEST);
        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        heroText.add(Widgets.label(Cfg.BUILD, Palette.semi(21f), Palette.FG));
        JLabel heroSub = Widgets.label("Minecraft " + Cfg.MC_VERSION + "  ·  " + Cfg.LOADER, Palette.reg(8.4f), Palette.MUTED);
        heroSub.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(Box.createVerticalStrut(3));
        heroText.add(heroSub);
        JLabel heroVer = Widgets.label("BerryUp " + Cfg.VERSION, Palette.semi(8.2f), Palette.ACCENT_HOVER);
        heroVer.setAlignmentX(LEFT_ALIGNMENT);
        heroText.add(Box.createVerticalStrut(2));
        heroText.add(heroVer);
        hero.add(heroText, BorderLayout.CENTER);
        side.add(hero);
        side.add(Box.createVerticalStrut(14));

        playButton = new Widgets.BerryButton("Играть", Widgets.BerryButton.Kind.PRIMARY);
        playButton.setAlignmentX(LEFT_ALIGNMENT);
        playButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        playButton.setFont(Palette.semi(11.5f));
        playButton.addActionListener(e -> play(false));
        side.add(playButton);
        side.add(Box.createVerticalStrut(8));

        connectButton = new Widgets.BerryButton("Играть с подключением к серверу", Widgets.BerryButton.Kind.SECONDARY);
        connectButton.setAlignmentX(LEFT_ALIGNMENT);
        connectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        connectButton.addActionListener(e -> play(true));
        side.add(connectButton);
        side.add(Box.createVerticalStrut(14));

        // ---------------------------------------------------------- карточка сервера
        Widgets.Card serverCard = new Widgets.Card();
        serverCard.setAlignmentX(LEFT_ALIGNMENT);
        serverCard.setLayout(new BoxLayout(serverCard, BoxLayout.Y_AXIS));
        serverCard.pad(15, 16, 15, 16);
        serverCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 214));
        serverCard.add(Widgets.caption("СЕРВЕР", Palette.SURFACE));
        serverCard.add(Box.createVerticalStrut(8));

        serverBox = new JComboBox<String>(serverNames());
        serverBox.setSelectedIndex(Store.serverIndex());
        serverBox.setFont(Palette.reg(9.5f));
        serverBox.setBackground(Palette.SURFACE_ALT);
        serverBox.setForeground(Palette.FG);
        serverBox.setFocusable(false);
        serverBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
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

        serverMotd = Widgets.paragraph("", Palette.reg(8.5f), Palette.DIM);
        serverMotd.setAlignmentX(LEFT_ALIGNMENT);
        serverMotd.setRows(3);
        serverCard.add(serverMotd);
        serverCard.add(Box.createVerticalGlue());

        Widgets.BerryButton copy = new Widgets.BerryButton("Скопировать адрес", Widgets.BerryButton.Kind.GHOST);
        copy.setAlignmentX(LEFT_ALIGNMENT);
        copy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
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
        quick.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        updateButton = new Widgets.BerryButton("Обновить сборку", Widgets.BerryButton.Kind.SECONDARY);
        updateButton.addActionListener(e -> updatePack());
        folderButton = new Widgets.BerryButton("Папка сборки", Widgets.BerryButton.Kind.SECONDARY);
        folderButton.addActionListener(e -> openInstanceFolder());
        quick.add(updateButton);
        quick.add(folderButton);
        side.add(quick);

        side.add(Box.createVerticalGlue());
        JLabel sideFoot = Widgets.label("данные: %APPDATA%\\BerryUp", Palette.reg(7.8f), Palette.DIM);
        sideFoot.setAlignmentX(LEFT_ALIGNMENT);
        side.add(sideFoot);

        root.add(side, BorderLayout.WEST);

        // ---------------------------------------------------------- правая часть
        nav = new Widgets.Segmented("Главное", "Аккаунт", "Настройки", "Диагностика", "О программе");
        nav.onSelect(this::showPage);
        nav.setAlignmentX(LEFT_ALIGNMENT);

        banner = new Banner();

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        header.add(nav, BorderLayout.WEST);
        Widgets.BerryButton site = new Widgets.BerryButton("Сайт сборки", Widgets.BerryButton.Kind.GHOST);
        site.addActionListener(e -> ru.berrytrip.berryup.Sys.openUrl(Cfg.SITE));
        JPanel siteWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        siteWrap.setOpaque(false);
        siteWrap.add(site);
        header.add(siteWrap, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(banner, BorderLayout.NORTH);
        top.add(header, BorderLayout.SOUTH);

        home = new HomePage(this);
        account = new AccountPage(this);
        settings = new SettingsPage(this);
        diag = new DiagPage(this);
        about = new AboutPage(this);

        pages.setOpaque(false);
        pages.setBorder(BorderFactory.createEmptyBorder(16, 2, 0, 2));
        pages.add(home, PAGE_NAMES[0]);
        pages.add(account, PAGE_NAMES[1]);
        pages.add(settings, PAGE_NAMES[2]);
        pages.add(diag, PAGE_NAMES[3]);
        pages.add(about, PAGE_NAMES[4]);

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
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
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
            Palette.fillRound(g, 0, 0, w, h, 14, Palette.mix(Palette.SURFACE, Palette.ACCENT, 0.12f));
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 14, Palette.alpha(Palette.ACCENT, 110));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Для тестов: выбрать вкладку и отрисовать её в PNG. */
    public JComponent[] tabs() {
        return new JComponent[]{home, account, settings, diag, about};
    }

    public Font heading() {
        return Palette.semi(14f);
    }
}
