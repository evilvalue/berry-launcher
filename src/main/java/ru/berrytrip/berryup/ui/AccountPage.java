package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Auth;
import ru.berrytrip.berryup.Log;
import ru.berrytrip.berryup.Skins;
import ru.berrytrip.berryup.Store;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/** Вкладка «Аккаунт»: вход через Telegram-бота, скин и плащ QuickSkin. */
public class AccountPage extends JPanel {

    private final MainFrame host;
    private final JLabel status;
    private final JTextArea hint;
    private final JTextField nickField;
    private final Widgets.PixelButton login;
    private final Widgets.PixelButton logout;
    private final Widgets.PixelButton check;
    private final JPanel secondaryActions;
    private final JCheckBox remember;

    private static final Color HERO_MUTED = new Color(0xC8BACF);
    private static final Color HERO_DIM = new Color(0x8E8197);
    private static final Color HERO_OK = new Color(0x5BE3A6);
    private static final Color HERO_WARN = new Color(0xFFC36B);
    private static final Color HERO_ERR = new Color(0xFF7D8A);

    private SkinView preview;
    private final Widgets.Segmented modelChoice;
    private String model = "auto";
    private final JLabel skinFile = Widgets.label("", Palette.semi(9.6f), Palette.FG);
    private final JLabel capeFile = Widgets.label("", Palette.semi(9.6f), Palette.FG);
    private final JLabel skinStatus = Widgets.label("", Palette.reg(8.8f), Palette.MUTED);
    private final Widgets.BerryButton applySkin;
    private final Widgets.BerryButton applyCape;
    private final Widgets.BerryButton clearCape;

    private Skins.Asset pickedSkin;
    private Skins.Asset pickedCape;
    private String activeSkinName = "";
    private String activeCapeName = "";
    private String shownSkinHash = "";
    private String shownCapeHash = "";
    private int lastState = -1;

    public AccountPage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BorderLayout(14, 0));

        // ---------------------------------------------------------- вход
        HeroPanel loginCard = new HeroPanel();
        loginCard.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.pad(24, 24, 20, 24);
        loginCard.setPreferredSize(new Dimension(380, 600));
        loginCard.setMinimumSize(new Dimension(350, 560));

        JPanel brandRow = new JPanel(new BorderLayout());
        brandRow.setOpaque(false);
        brandRow.setAlignmentX(LEFT_ALIGNMENT);
        brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new PixelMark(26));
        brand.add(Box.createHorizontalStrut(10));
        brand.add(Widgets.label("BERRYTRIP ID", Palette.semi(10f), Color.WHITE));
        brandRow.add(brand, BorderLayout.WEST);
        loginCard.add(brandRow);
        loginCard.add(Box.createVerticalStrut(24));

        JLabel title = Widgets.label("Вход в игру", Palette.semi(29f), Color.WHITE);
        loginCard.add(left(title));
        loginCard.add(Box.createVerticalStrut(12));

        status = Widgets.label("ВХОД НЕ ВЫПОЛНЕН", Palette.semi(9.4f), HERO_WARN);
        status.setOpaque(true);
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        status.setBackground(Palette.alpha(HERO_WARN, 34));
        status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        status.addPropertyChangeListener("foreground", event -> {
            if (event.getNewValue() instanceof Color) {
                Color tone = (Color) event.getNewValue();
                status.setBackground(Palette.alpha(tone, 34));
            }
        });
        loginCard.add(left(status));
        loginCard.add(Box.createVerticalStrut(8));

        hint = Widgets.paragraph("", Palette.reg(8.7f), HERO_MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        hint.setVisible(false);
        loginCard.add(hint);
        loginCard.add(Box.createVerticalStrut(8));

        nickField = new JTextField();
        nickField.setFont(Palette.reg(10.5f));
        nickField.setBackground(new Color(0x241C28));
        nickField.setForeground(Color.WHITE);
        nickField.setCaretColor(Palette.ACCENT_2);
        nickField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.alpha(Color.WHITE, 55), 1),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        nickField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nickField.setPreferredSize(new Dimension(300, 44));
        nickField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent event) {
                nickField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Palette.ACCENT_2, 1),
                        BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                nickField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Palette.alpha(Color.WHITE, 55), 1),
                        BorderFactory.createEmptyBorder(9, 12, 9, 12)));
            }
        });
        nickField.putClientProperty("JTextField.placeholderText", "Ник в Minecraft");
        nickField.setToolTipText("Ник в Minecraft, который нужно привязать к Telegram");
        loginCard.add(nickField);
        loginCard.add(Box.createVerticalStrut(10));

        login = new Widgets.PixelButton("Войти через Telegram", Widgets.PixelButton.Tone.PRIMARY).telegram();
        login.setAlignmentX(LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        login.addActionListener(e -> startLogin());
        loginCard.add(login);
        loginCard.add(Box.createVerticalStrut(7));

        secondaryActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        secondaryActions.setOpaque(false);
        secondaryActions.setAlignmentX(LEFT_ALIGNMENT);
        secondaryActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        check = new Widgets.PixelButton("Проверить вход", Widgets.PixelButton.Tone.SECONDARY).compact();
        check.addActionListener(e -> pollNow(false));
        logout = new Widgets.PixelButton("Выйти", Widgets.PixelButton.Tone.GHOST).compact();
        logout.addActionListener(e -> {
            Auth.logout(host.prism());
            refresh();
            host.setState("Вход сброшен", Palette.MUTED);
        });
        secondaryActions.add(check);
        secondaryActions.add(logout);
        loginCard.add(secondaryActions);
        loginCard.add(Box.createVerticalStrut(8));

        remember = new JCheckBox("Запомнить вход");
        remember.setOpaque(false);
        remember.setFont(Palette.reg(8.6f));
        remember.setForeground(HERO_MUTED);
        remember.setFocusPainted(false);
        remember.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        remember.setAlignmentX(LEFT_ALIGNMENT);
        remember.setSelected(Store.remember());
        remember.setToolTipText("Токен входа лежит в сборке. Выключи на общем компьютере — тогда при следующем запуске BerryUp попросит вход заново.");
        remember.addActionListener(e -> {
            Store.setRemember(remember.isSelected());
            Store.save();
            host.setState(remember.isSelected()
                            ? "Вход сохранится на этом компьютере"
                            : "Вход не сохранится: при следующем запуске попросим заново",
                    Palette.MUTED);
            refresh();
        });
        loginCard.add(remember);
        loginCard.add(Box.createVerticalGlue());

        // ---------------------------------------------------------- внешний вид
        LookCard lookCard = new LookCard();
        lookCard.setAlignmentX(LEFT_ALIGNMENT);
        lookCard.setLayout(new BorderLayout(16, 12));
        lookCard.pad(18, 20, 16, 20);
        lookCard.setPreferredSize(new Dimension(470, 600));

        JPanel lookTop = new JPanel(new BorderLayout());
        lookTop.setOpaque(false);
        lookTop.add(Widgets.caption("ВНЕШНИЙ ВИД", Palette.SURFACE), BorderLayout.WEST);

        JPanel modelBox = new JPanel();
        modelBox.setOpaque(false);
        modelBox.setLayout(new BoxLayout(modelBox, BoxLayout.Y_AXIS));
        modelBox.add(Widgets.caption("МОДЕЛЬ", Palette.SURFACE));
        modelBox.add(Box.createVerticalStrut(3));
        modelChoice = new Widgets.Segmented("AUTO", "CLASSIC", "SLIM");
        modelChoice.setAlignmentX(LEFT_ALIGNMENT);
        modelChoice.onSelect(index -> {
            model = modelValue(index);
            Skins.setModel(host.prism(), model);
            preview.setSlim(resolvedSlim());
            host.setState("Модель: " + modelLabel(model), Palette.MUTED);
        });
        modelBox.add(modelChoice);
        lookTop.add(modelBox, BorderLayout.EAST);
        lookTop.add(skinStatus, BorderLayout.SOUTH);
        skinStatus.setVisible(false);
        lookCard.add(lookTop, BorderLayout.NORTH);

        LookStage stage = new LookStage();
        stage.setPreferredSize(new Dimension(250, 380));
        stage.setMinimumSize(new Dimension(210, 300));
        preview = new SkinView();
        preview.setPreferredSize(new Dimension(250, 330));
        stage.add(preview, BorderLayout.CENTER);
        stage.setToolTipText("Потяни модель мышью, чтобы повернуть");
        lookCard.add(stage, BorderLayout.CENTER);

        JPanel slots = new JPanel();
        slots.setOpaque(false);
        slots.setLayout(new BoxLayout(slots, BoxLayout.Y_AXIS));
        slots.setPreferredSize(new Dimension(176, 430));
        slots.setMaximumSize(new Dimension(176, Integer.MAX_VALUE));

        slots.add(Widgets.caption("СКИН", Palette.SURFACE));
        slots.add(Box.createVerticalStrut(4));
        slots.add(left(skinFile));
        slots.add(Box.createVerticalStrut(8));
        Widgets.BerryButton pickSkin = new Widgets.BerryButton("Выбрать", Widgets.BerryButton.Kind.SECONDARY);
        pickSkin.setToolTipText("PNG или GIF");
        pickSkin.setAlignmentX(LEFT_ALIGNMENT);
        pickSkin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        pickSkin.addActionListener(e -> choose(false));
        applySkin = new Widgets.BerryButton("Применить", Widgets.BerryButton.Kind.PRIMARY);
        applySkin.setAlignmentX(LEFT_ALIGNMENT);
        applySkin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        applySkin.setEnabled(false);
        applySkin.addActionListener(e -> apply(false));
        slots.add(pickSkin);
        slots.add(Box.createVerticalStrut(5));
        slots.add(applySkin);

        slots.add(Box.createVerticalStrut(12));
        JSeparator line = new JSeparator();
        line.setForeground(Palette.LINE);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        slots.add(line);
        slots.add(Box.createVerticalStrut(12));

        slots.add(Widgets.caption("ПЛАЩ", Palette.SURFACE));
        slots.add(Box.createVerticalStrut(4));
        slots.add(left(capeFile));
        slots.add(Box.createVerticalStrut(8));
        Widgets.BerryButton pickCape = new Widgets.BerryButton("Выбрать", Widgets.BerryButton.Kind.SECONDARY);
        pickCape.setToolTipText("PNG или GIF");
        pickCape.setAlignmentX(LEFT_ALIGNMENT);
        pickCape.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        pickCape.addActionListener(e -> choose(true));
        applyCape = new Widgets.BerryButton("Применить", Widgets.BerryButton.Kind.PRIMARY);
        applyCape.setAlignmentX(LEFT_ALIGNMENT);
        applyCape.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        applyCape.setEnabled(false);
        applyCape.addActionListener(e -> apply(true));
        clearCape = new Widgets.BerryButton("Снять", Widgets.BerryButton.Kind.GHOST);
        clearCape.setAlignmentX(LEFT_ALIGNMENT);
        clearCape.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        clearCape.addActionListener(e -> removeCape());
        slots.add(pickCape);
        slots.add(Box.createVerticalStrut(5));
        slots.add(applyCape);
        slots.add(Box.createVerticalStrut(5));
        slots.add(clearCape);

        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.add(slots, BorderLayout.NORTH);
        JPanel lookBody = new JPanel(new BorderLayout(16, 0));
        lookBody.setOpaque(false);
        lookBody.add(stage, BorderLayout.CENTER);
        lookBody.add(side, BorderLayout.EAST);
        lookCard.add(lookBody, BorderLayout.CENTER);

        javax.swing.TransferHandler drop = new javax.swing.TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                    return false;
                }
                flashDrop();
                return true;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<File> files = (java.util.List<File>) support.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (files.isEmpty()) return false;
                    accept(files.get(0));
                    return true;
                } catch (Exception error) {
                    skinStatus.setText("Не удалось принять файл: " + error.getMessage());
                    skinStatus.setForeground(Palette.ERR);
                    return false;
                }
            }
        };
        installDrop(lookCard, drop);

        add(loginCard, BorderLayout.WEST);
        add(lookCard, BorderLayout.CENTER);

        // При изменении размера окна перерисовываем страницу целиком:
        // иначе старый отрисованный чекбокс может остаться на прежнем месте.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                repaint();
            }
        });

        loadExisting();
        refresh();

        // Пока висит ссылка, тихо спрашиваем сервер: бот уже мог нажать Start.
        javax.swing.Timer poll = new javax.swing.Timer(2000, event -> {
            if (Auth.pending()) {
                pollNow(true);
            } else {
                int state = Auth.tokenState(host.prism());
                if (state != lastState) {
                    refresh();
                }
            }
        });
        poll.start();
    }

    private void setHint(String text) {
        hint.setText(text == null ? "" : text);
        hint.setVisible(text != null && !text.isBlank());
        revalidate();
    }

    private <T extends JComponent> T left(T component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        return component;
    }

    /** Показывает именно тот скин и плащ, которые сейчас выбраны в игре. */
    private void loadExisting() {
        Skins.Asset skin = Skins.currentAsset(host.prism(), false);
        Skins.Asset cape = Skins.currentAsset(host.prism(), true);
        preview.setSkin(skin == null ? null : skin.image);
        preview.setCape(cape == null ? null : cape.image);
        activeSkinName = skin == null ? "" : skin.file.getName();
        activeCapeName = Skins.activeCapeLabel(host.prism());
        shownSkinHash = safeHash(Skins.activeHash(host.prism(), false));
        shownCapeHash = safeHash(Skins.activeHash(host.prism(), true));
        pickedSkin = null;
        pickedCape = null;
        model = Skins.model(host.prism());
        modelChoice.select(modelIndex(model), false);
        preview.setSlim(resolvedSlim());
        updateSkinLabels();
        updateCapeLabels();
    }

    /** Если скин поменяли в игре, превью подтягивает новый выбор из конфига мода. */
    private void syncLook() {
        String skinHash = safeHash(Skins.activeHash(host.prism(), false));
        String capeHash = safeHash(Skins.activeHash(host.prism(), true));
        if (skinHash.equals(shownSkinHash) && capeHash.equals(shownCapeHash)) return;
        loadExisting();
    }

    private static String safeHash(String value) {
        return value == null ? "" : value;
    }

    public void refresh() {
        int state = Auth.tokenState(host.prism());
        lastState = state;
        String nick = Auth.tokenNick(host.prism());
        if (nick.isEmpty()) nick = Auth.nick();
        if (state == Auth.TOKEN_OK) {
            status.setText("ВХОД ВЫПОЛНЕН" + (nick.isEmpty() ? "" : "  ·  " + nick));
            status.setForeground(HERO_OK);
            setHint("");
        } else if (Auth.pending()) {
            String waiting = Auth.pendingNick();
            status.setText("ЖДЁМ START В TELEGRAM" + (waiting.isEmpty() ? "" : "  ·  " + waiting));
            status.setForeground(Palette.ACCENT_2);
            setHint("Подтверди вход в Telegram");
        } else {
            status.setText(state == Auth.TOKEN_EXPIRED ? "СЕССИЯ ИСТЕКЛА — ВОЙДИ СНОВА"
                    : state == Auth.TOKEN_OTHER_NICK ? "ТОКЕН ВЫДАН ДРУГОМУ НИКУ"
                    : state == Auth.TOKEN_BROKEN ? "ТОКЕН ПОВРЕЖДЁН — ВОЙДИ СНОВА"
                    : "ВХОД НЕ ВЫПОЛНЕН");
            status.setForeground(HERO_WARN);
            setHint(state == Auth.TOKEN_NONE ? "" : "Сохранённый токен больше не подходит");
        }
        boolean signed = state == Auth.TOKEN_OK;
        login.setText(signed ? "Войти заново"
                : Auth.pending() ? "Открыть Telegram снова" : "Войти через Telegram");
        check.setVisible(Auth.pending());
        logout.setVisible(signed || Auth.pending());
        secondaryActions.setVisible(check.isVisible() || logout.isVisible());
        remember.setSelected(Store.remember());
        if (nickField.getText().trim().isEmpty()) {
            String name = Auth.nick();
            if (name.isEmpty() && host.prism() != null) name = host.prism().accountName();
            nickField.setText(name);
        }
        syncLook();
        revalidate();
        repaint();
    }

    private static String modelValue(int index) {
        return index == 1 ? "classic" : index == 2 ? "slim" : "auto";
    }

    private static int modelIndex(String value) {
        return "classic".equals(value) ? 1 : "slim".equals(value) ? 2 : 0;
    }

    private static String modelLabel(String value) {
        return "classic".equals(value) ? "Classic" : "slim".equals(value) ? "Slim" : "Auto";
    }

    private boolean resolvedSlim() {
        if ("slim".equals(model)) return true;
        if ("classic".equals(model)) return false;
        return SkinView.detectSlim(preview == null ? null : preview.skin());
    }

    private String tokenMessage(int state) {
        if (state == Auth.TOKEN_OK) return "Вход подтверждён";
        if (state == Auth.TOKEN_EXPIRED) return "Токен истёк — войди снова";
        if (state == Auth.TOKEN_OTHER_NICK) return "Токен выдан другому нику";
        if (state == Auth.TOKEN_BROKEN) return "Файл токена повреждён";
        return "Токена пока нет";
    }

    private boolean polling;

    private void startLogin() {
        String nick = nickField.getText().trim();
        if (nick.isEmpty()) {
            status.setText("Напиши ник из Minecraft");
            status.setForeground(HERO_WARN);
            return;
        }
        if (Auth.pending() && Auth.openPending()) {
            status.setText("Telegram открыт — нажми Start");
            status.setForeground(Palette.ACCENT_2);
            return;
        }
        login.setEnabled(false);
        status.setText("Создаю ссылку на сервере…");
        status.setForeground(HERO_WARN);
        setHint("");
        final String wanted = nick;
        new Thread(() -> {
            String error = Auth.start(host.prism(), wanted);
            SwingUtilities.invokeLater(() -> {
                login.setEnabled(true);
                if (!error.isEmpty()) {
                    if (error.startsWith("Сервер входа")) {
                        status.setText("СЕРВЕР ВХОДА НЕДОСТУПЕН");
                        status.setForeground(HERO_ERR);
                        setHint(error);
                    } else {
                        status.setText("ПРОВЕРЬ НИК");
                        status.setForeground(HERO_ERR);
                        setHint(error);
                    }
                    host.setState("Вход не начат", Palette.ERR);
                    return;
                }
                status.setText("ЖДЁМ START В TELEGRAM");
                status.setForeground(Palette.ACCENT_2);
                setHint("Telegram открыт");
                host.setState("Жду Start в Telegram", Palette.ACCENT);
                refresh();
            });
        }, "berry-auth-start").start();
    }

    /** Ручная проверка, не дожидаясь таймера. */
    private void pollNow(boolean silent) {
        if (polling) return;
        if (!Auth.pending()) {
            refresh();
            int state = Auth.tokenState(host.prism());
            host.setState(tokenMessage(state), state == Auth.TOKEN_OK ? Palette.OK : Palette.WARN);
            return;
        }
        polling = true;
        if (!silent) {
            status.setText("Проверяю вход на сервере…");
            status.setForeground(HERO_WARN);
        }
        new Thread(() -> {
            Auth.Poll result = Auth.poll(host.prism());
            SwingUtilities.invokeLater(() -> {
                polling = false;
                if (result.state == Auth.POLL_OK) {
                    refresh();
                    host.setState("Вход подтверждён — пустим в игру", Palette.OK);
                } else if (result.state == Auth.POLL_ERROR || result.state == Auth.POLL_EXPIRED) {
                    refresh();
                    status.setText(result.message.isEmpty() ? "ВХОД НЕ ПОДТВЕРЖДЁН" : "ВХОД НЕ ПОДТВЕРЖДЁН");
                    status.setForeground(HERO_ERR);
                    setHint(result.message.isEmpty() ? "Начни вход заново" : result.message);
                    host.setState("Вход не подтверждён", Palette.ERR);
                } else if (result.state == Auth.POLL_WAITING) {
                    if (!silent) {
                        status.setText("Жду Start в Telegram"
                                + (result.nick.isEmpty() ? "" : " · " + result.nick));
                        status.setForeground(Palette.ACCENT_2);
                    }
                }
            });
        }, "berry-auth-poll").start();
    }

    private static File lastDir;
    private javax.swing.Timer dropTimer;

    /**
     * Проводник только с файлами QuickSkin: PNG/GIF, без «всех файлов»,
     * с превью и проверкой размера выбранной картинки справа.
     */
    private void choose(boolean cape) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(cape ? "Выбери плащ для QuickSkin" : "Выбери скин для QuickSkin");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileHidingEnabled(true);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                (cape ? "Плащи QuickSkin — PNG или GIF" : "Скины QuickSkin — PNG или GIF"), "png", "gif");
        chooser.setFileFilter(filter);

        AssetPreview accessory = new AssetPreview(cape);
        chooser.setAccessory(accessory);
        chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY,
                event -> accessory.inspect((File) event.getNewValue()));

        chooser.setCurrentDirectory(startDir());
        chooser.setApproveButtonText("Выбрать");
        int result = chooser.showOpenDialog(this);
        File file = chooser.getSelectedFile();
        if (result != JFileChooser.APPROVE_OPTION || file == null || !file.isFile()) return;
        lastDir = file.getParentFile();
        if (cape) useCape(file); else useSkin(file);
    }

    private File startDir() {
        if (lastDir != null && lastDir.isDirectory()) return lastDir;
        String home = System.getProperty("user.home", ".");
        File downloads = new File(home, "Downloads");
        return downloads.isDirectory() ? downloads : new File(home);
    }

    /** Подсвечивает сцену, пока над карточкой держат файл. */
    private void flashDrop() {
        preview.setDropActive(true);
        if (dropTimer != null) dropTimer.stop();
        dropTimer = new javax.swing.Timer(650, event -> preview.setDropActive(false));
        dropTimer.setRepeats(false);
        dropTimer.start();
    }

    /** Ставит приём файлов на карточку и все её элементы, кроме текстовых полей. */
    private void installDrop(Component component, javax.swing.TransferHandler handler) {
        if (component instanceof javax.swing.text.JTextComponent) return;
        if (component instanceof JComponent) ((JComponent) component).setTransferHandler(handler);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                installDrop(child, handler);
            }
        }
    }

    /** Один путь для кнопки и для drag&drop: сам понимает, скин это или плащ. */
    private void accept(File file) {
        try {
            showSkin(Skins.readSkin(file));
            return;
        } catch (Exception skinError) {
            try {
                showCape(Skins.readCape(file));
            } catch (Exception capeError) {
                skinStatus.setText("QuickSkin не примет файл: " + skinError.getMessage());
                skinStatus.setForeground(Palette.ERR);
                skinStatus.setVisible(true);
                host.setState("Формат не подходит QuickSkin", Palette.ERR);
            }
        }
    }

    private void useSkin(File file) {
        try {
            showSkin(Skins.readSkin(file));
        } catch (Exception error) {
            pickedSkin = null;
            applySkin.setEnabled(false);
            updateSkinLabels();
            skinStatus.setText(error.getMessage());
            skinStatus.setForeground(Palette.ERR);
            skinStatus.setVisible(true);
            host.setState(error.getMessage(), Palette.ERR);
        }
    }

    private void useCape(File file) {
        try {
            showCape(Skins.readCape(file));
        } catch (Exception error) {
            pickedCape = null;
            applyCape.setEnabled(false);
            updateCapeLabels();
            skinStatus.setText(error.getMessage());
            skinStatus.setForeground(Palette.ERR);
            skinStatus.setVisible(true);
            host.setState(error.getMessage(), Palette.ERR);
        }
    }

    private void showSkin(Skins.Asset asset) {
        pickedSkin = asset;
        preview.setSkin(asset.image);
        preview.setSlim(resolvedSlim());
        updateSkinLabels();
        skinStatus.setVisible(false);
        host.setState("Скин примерен", Palette.MUTED);
    }

    private void showCape(Skins.Asset asset) {
        pickedCape = asset;
        preview.setCape(asset.image);
        updateCapeLabels();
        skinStatus.setVisible(false);
        host.setState("Плащ примерен", Palette.MUTED);
    }

    private void updateSkinLabels() {
        if (pickedSkin == null) {
            skinFile.setText(activeSkinName.isEmpty() ? "Не выбран" : activeSkinName);
            skinFile.setToolTipText(activeSkinName.isEmpty()
                    ? null : "Скин выбран в игре · QuickSkin");
            applySkin.setEnabled(false);
        } else {
            skinFile.setText(pickedSkin.file.getName());
            skinFile.setToolTipText(pickedSkin.file.getName() + " · " + pickedSkin.sizeText());
            applySkin.setEnabled(true);
        }
    }

    private void updateCapeLabels() {
        if (pickedCape == null) {
            capeFile.setText(activeCapeName.isEmpty() ? "Не выбран" : activeCapeName);
            capeFile.setToolTipText(activeCapeName.isEmpty()
                    ? null : "Плащ выбран в меню QuickSkin");
            applyCape.setEnabled(false);
        } else {
            capeFile.setText(pickedCape.file.getName());
            capeFile.setToolTipText(pickedCape.file.getName() + " · " + pickedCape.sizeText());
            applyCape.setEnabled(true);
        }
    }

    private void apply(boolean cape) {
        Skins.Asset asset = cape ? pickedCape : pickedSkin;
        if (asset == null) return;
        if (cape) applyCape.setEnabled(false); else applySkin.setEnabled(false);
        skinStatus.setText("Копирую…");
        skinStatus.setForeground(Palette.MUTED);
        skinStatus.setVisible(true);
        new Thread(() -> {
            String message;
            boolean ok = true;
            try {
                String name = cape
                        ? Skins.applyCape(host.prism(), asset.file)
                        : Skins.applySkin(host.prism(), asset.file, Auth.nick(), model);
                message = (cape ? "Плащ в QuickSkin: " : "Скин в QuickSkin: ") + name;
                Log.ok(message);
            } catch (Exception error) {
                ok = false;
                message = "Не получилось: " + error.getMessage();
                Log.error(message);
            }
            final String text = message;
            final boolean success = ok;
            SwingUtilities.invokeLater(() -> {
                if (cape) applyCape.setEnabled(true); else applySkin.setEnabled(true);
                skinStatus.setText(success ? "" : text);
                skinStatus.setForeground(success ? Palette.OK : Palette.ERR);
                skinStatus.setVisible(!success);
                if (success) loadExisting();
                host.setState(text, success ? Palette.OK : Palette.ERR);
            });
        }, "berry-skin").start();
    }

    private void removeCape() {
        Skins.clearCape(host.prism());
        pickedCape = null;
        preview.setCape(null);
        updateCapeLabels();
        skinStatus.setVisible(false);
    }

    /** Превью выбранного файла прямо в проводнике: что это и подходит ли. */
    private static final class AssetPreview extends JPanel {
        private final boolean cape;
        private final JLabel thumb = new JLabel("", javax.swing.SwingConstants.CENTER);
        private final JLabel name = Widgets.label("", Palette.semi(9.8f), Palette.FG);
        private final JLabel size = Widgets.label("", Palette.reg(8.6f), Palette.MUTED);
        private final JLabel verdict = Widgets.label("", Palette.reg(8.6f), Palette.DIM);

        AssetPreview(boolean cape) {
            this.cape = cape;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 1, 0, 0, Palette.LINE),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)));
            setPreferredSize(new Dimension(218, 242));

            thumb.setAlignmentX(LEFT_ALIGNMENT);
            thumb.setPreferredSize(new Dimension(190, 108));
            thumb.setMaximumSize(new Dimension(190, 108));
            thumb.setOpaque(true);
            thumb.setBackground(Palette.SURFACE_ALT);
            thumb.setBorder(BorderFactory.createLineBorder(Palette.LINE, 1, true));
            add(thumb);
            add(Box.createVerticalStrut(10));

            name.setAlignmentX(LEFT_ALIGNMENT);
            add(name);
            add(Box.createVerticalStrut(3));
            size.setAlignmentX(LEFT_ALIGNMENT);
            add(size);
            add(Box.createVerticalStrut(8));
            verdict.setAlignmentX(LEFT_ALIGNMENT);
            add(verdict);

            inspect(null);
        }

        void inspect(File file) {
            if (file == null || !file.isFile()) {
                thumb.setIcon(null);
                name.setText(cape ? "Плащ QuickSkin" : "Скин QuickSkin");
                size.setText("PNG или GIF");
                verdict.setText("Выбери файл — покажу размер и подойдёт ли он.");
                verdict.setForeground(Palette.DIM);
                revalidate();
                repaint();
                return;
            }
            name.setText(file.getName());
            try {
                Skins.Asset asset = cape ? Skins.readCape(file) : Skins.readSkin(file);
                thumb.setIcon(new ImageIcon(thumbnail(asset.image, 190, 108)));
                size.setText(asset.sizeText());
                verdict.setText(cape ? "Подходит: плащ QuickSkin" : "Подходит: скин QuickSkin");
                verdict.setForeground(Palette.OK);
            } catch (Exception error) {
                thumb.setIcon(null);
                size.setText("");
                String text = error.getMessage() == null ? "не подходит" : error.getMessage();
                verdict.setText("<html><div style='width:176px'>" + text.replace("<", "&lt;") + "</div></html>");
                verdict.setForeground(Palette.ERR);
            }
            revalidate();
            repaint();
        }

        private static java.awt.Image thumbnail(BufferedImage image, int maxW, int maxH) {
            double ratio = Math.min(maxW / (double) image.getWidth(), maxH / (double) image.getHeight());
            int w = Math.max(1, (int) Math.round(image.getWidth() * ratio));
            int h = Math.max(1, (int) Math.round(image.getHeight() * ratio));
            BufferedImage out = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, (maxW - w) / 2, (maxH - h) / 2, w, h, null);
            g.dispose();
            return out;
        }
    }

    // ------------------------------------------------------------ сцена входа

    private static java.awt.Shape pixelShape(int x, int y, int w, int h, int cut) {
        int c = Math.max(1, cut);
        return new java.awt.Polygon(
                new int[]{x + c, x + w - c, x + w, x + w, x + w - c, x + c, x, x},
                new int[]{y, y, y + c, y + h - c, y + h, y + h, y + h - c, y + c}, 8);
    }

    private static void paintGrid(Graphics2D g, int w, int h, int step, Color color) {
        g.setColor(color);
        for (int x = 0; x < w; x += step) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += step) g.drawLine(0, y, w, y);
    }

    private static final class HeroPanel extends JPanel {
        HeroPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
        }

        void pad(int top, int left, int bottom, int right) {
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            if (w <= 2 || h <= 2) {
                g.dispose();
                return;
            }
            java.awt.Shape outer = pixelShape(0, 0, w - 4, h - 4, 7);
            g.setColor(new Color(0, 0, 0, 82));
            g.fill(pixelShape(4, 5, w - 5, h - 5, 7));
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x25182B), 0, h, new Color(0x100D15)));
            g.fill(outer);
            g.clip(outer);
            paintGrid(g, w, h, 28, new Color(255, 255, 255, 8));
            g.setColor(Palette.alpha(Palette.ACCENT, 12));
            g.drawRect(w - 92, h - 118, 54, 54);
            g.drawRect(w - 104, h - 106, 18, 18);
            int block = 16;
            int bx = w - 142;
            int by = h - 92;
            g.setColor(Palette.alpha(Palette.ACCENT, 16));
            g.fillRect(bx, by, block, block);
            g.fillRect(bx + block, by + block, block, block);
            g.fillRect(bx + block * 2, by, block, block);
            g.fillRect(bx + block, by + block * 2, block, block);
            g.setColor(Palette.alpha(Palette.ACCENT_2, 22));
            g.fillRect(bx + block, by - block, block, block);
            g.fillRect(bx + block * 2, by - block, block, block);
            g.dispose();
            g = Palette.quality(graphics);
            g.setStroke(new java.awt.BasicStroke(1f));
            g.setColor(Palette.alpha(Palette.ACCENT, 92));
            g.draw(outer);
            g.setColor(Palette.ACCENT);
            g.fillRect(0, 0, 4, Math.max(1, h - 4));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class LookCard extends JPanel {
        LookCard() {
            setOpaque(false);
            setLayout(new BorderLayout());
        }

        void pad(int top, int left, int bottom, int right) {
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            if (w <= 2 || h <= 2) {
                g.dispose();
                return;
            }
            java.awt.Shape outer = pixelShape(0, 0, w - 3, h - 3, 5);
            g.setColor(Palette.alpha(Color.BLACK, Palette.isDark() ? 70 : 12));
            g.fill(pixelShape(3, 4, w - 4, h - 4, 5));
            g.setPaint(Palette.cardGradient(w, h));
            g.fill(outer);
            g.clip(outer);
            paintGrid(g, w, h, 34, Palette.alpha(Palette.LINE, 45));
            g.setColor(Palette.alpha(Palette.ACCENT, 7));
            g.fillRect(w - 76, 0, 76, 76);
            g.dispose();
            g = Palette.quality(graphics);
            g.setStroke(new java.awt.BasicStroke(1f));
            g.setColor(Palette.LINE);
            g.draw(outer);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class LookStage extends JPanel {
        LookStage() {
            setOpaque(false);
            setLayout(new BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            if (w <= 2 || h <= 2) {
                g.dispose();
                return;
            }
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x241B2A), 0, h,
                    new Color(0x121017)));
            g.fillRect(0, 0, w, h);
            for (int y = 0; y < h; y += 14) {
                for (int x = 0; x < w; x += 14) {
                    if (((x / 14) + (y / 14)) % 2 == 0) {
                        g.setColor(new Color(255, 255, 255, 7));
                        g.fillRect(x, y, 14, 14);
                    }
                }
            }
            int platform = (int) (h * 0.80f);
            g.setColor(Palette.alpha(Palette.ACCENT, 18));
            g.fillRect(w / 2 - w / 5, platform, w / 2 + 12, 6);
            g.setColor(Palette.alpha(Palette.ACCENT, 35));
            g.fillRect(w / 2 - w / 7, platform + 6, w / 2 + 4, 3);
            g.setColor(Palette.alpha(Palette.ACCENT, 95));
            int tick = 7;
            g.drawLine(6, 6, 6 + tick, 6);
            g.drawLine(6, 6, 6, 6 + tick);
            g.drawLine(w - 7, h - 7, w - 7 - tick, h - 7);
            g.drawLine(w - 7, h - 7, w - 7, h - 7 - tick);
            g.setColor(Palette.alpha(Palette.ACCENT, 58));
            g.drawRect(0, 0, w - 1, h - 1);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class PixelMark extends JComponent {
        PixelMark(int size) {
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int size = Math.min(getWidth(), getHeight());
            g.setPaint(Palette.accentGradient(0, 0, size, size));
            g.fill(pixelShape(0, 0, size - 1, size - 1, 3));
            g.setColor(Color.WHITE);
            g.fillRect(size / 4, size / 4, 3, 3);
            g.fillRect(size / 4 + 5, size / 4, 3, 3);
            g.fillRect(size / 4, size / 4 + 5, 3, 3);
            g.fillRect(size / 4 + 5, size / 4 + 5, 3, 3);
            g.dispose();
        }
    }

}
