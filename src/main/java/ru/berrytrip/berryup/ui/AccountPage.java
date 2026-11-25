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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
    private final Widgets.BerryButton login;
    private final Widgets.BerryButton logout;
    private final Widgets.BerryButton check;
    private final JCheckBox remember;

    private final SkinView preview;
    private final JLabel skinFile = Widgets.label("", Palette.semi(10f), Palette.FG);
    private final JLabel skinMeta = Widgets.label("", Palette.reg(8.8f), Palette.DIM);
    private final JLabel capeFile = Widgets.label("", Palette.semi(10f), Palette.FG);
    private final JLabel capeMeta = Widgets.label("", Palette.reg(8.8f), Palette.DIM);
    private final JLabel skinStatus = Widgets.label("", Palette.reg(8.8f), Palette.MUTED);
    private final Widgets.BerryButton applySkin;
    private final Widgets.BerryButton applyCape;
    private final Widgets.BerryButton clearCape;

    private Skins.Asset pickedSkin;
    private Skins.Asset pickedCape;
    private int lastState = -1;

    public AccountPage(MainFrame host) {
        this.host = host;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // ---------------------------------------------------------- вход
        Widgets.Card loginCard = new Widgets.Card();
        loginCard.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.pad(20, 22, 18, 22);
        loginCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));

        JPanel loginHead = new JPanel(new BorderLayout());
        loginHead.setOpaque(false);
        loginHead.setAlignmentX(LEFT_ALIGNMENT);
        loginHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        loginHead.add(Widgets.caption("ВХОД ЧЕРЕЗ TELEGRAM", Palette.SURFACE), BorderLayout.WEST);
        loginHead.add(Widgets.label("BerryTrip ID", Palette.reg(8.6f), Palette.DIM), BorderLayout.EAST);
        loginCard.add(loginHead);
        loginCard.add(Box.createVerticalStrut(8));

        status = Widgets.label("Вход не выполнен", Palette.semi(14f), Palette.WARN);
        loginCard.add(left(status));
        loginCard.add(Box.createVerticalStrut(4));

        hint = Widgets.paragraph(
                "Нажми «Войти» — откроется бот. Нажми в нём Start, и сервер пустит в игру сам.",
                Palette.reg(9.2f), Palette.MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginCard.add(hint);
        loginCard.add(Box.createVerticalStrut(12));

        JPanel nickRow = new JPanel(new BorderLayout(10, 0));
        nickRow.setOpaque(false);
        nickRow.setAlignmentX(LEFT_ALIGNMENT);
        nickRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nickField = new JTextField();
        nickField.setFont(Palette.reg(10f));
        nickField.setBackground(Palette.SURFACE_ALT);
        nickField.setForeground(Palette.FG);
        nickField.setCaretColor(Palette.FG);
        nickField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.LINE, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        nickField.putClientProperty("JTextField.placeholderText", "Ник в Minecraft");
        nickField.setToolTipText("Ник в Minecraft, который нужно привязать к Telegram");
        nickRow.add(nickField, BorderLayout.CENTER);
        login = new Widgets.BerryButton("Войти через Telegram", Widgets.BerryButton.Kind.PRIMARY);
        login.addActionListener(e -> startLogin());
        nickRow.add(login, BorderLayout.EAST);
        loginCard.add(nickRow);
        loginCard.add(Box.createVerticalStrut(10));

        remember = new JCheckBox("Запомнить меня на этом компьютере");
        remember.setOpaque(false);
        remember.setFont(Palette.reg(9f));
        remember.setForeground(Palette.MUTED);
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
        loginCard.add(Box.createVerticalStrut(10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        Widgets.BerryButton openBot = new Widgets.BerryButton("Открыть бота", Widgets.BerryButton.Kind.SECONDARY);
        openBot.addActionListener(e -> startLogin());
        check = new Widgets.BerryButton("Проверить вход", Widgets.BerryButton.Kind.GHOST);
        check.addActionListener(e -> {
            refresh();
            int state = Auth.tokenState(host.prism());
            host.setState(tokenMessage(state), state == Auth.TOKEN_OK ? Palette.OK : Palette.WARN);
        });
        logout = new Widgets.BerryButton("Выйти", Widgets.BerryButton.Kind.GHOST);
        logout.addActionListener(e -> {
            Auth.logout(host.prism());
            refresh();
            host.setState("Вход сброшен", Palette.MUTED);
        });
        actions.add(openBot);
        actions.add(check);
        actions.add(logout);
        loginCard.add(actions);

        JLabel fine = Widgets.label(
                "Ссылку из бота никому не пересылай: она пускает в игру под твоим ником.",
                Palette.reg(8.2f), Palette.DIM);
        fine.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(Box.createVerticalStrut(4));
        loginCard.add(fine);

        add(loginCard);
        add(Box.createVerticalStrut(14));

        // ---------------------------------------------------------- внешний вид
        Widgets.Card lookCard = new Widgets.Card();
        lookCard.setAlignmentX(LEFT_ALIGNMENT);
        lookCard.setLayout(new BorderLayout(20, 14));
        lookCard.pad(20, 22, 18, 22);
        lookCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 470));
        lookCard.setMinimumSize(new Dimension(0, 390));
        lookCard.setPreferredSize(new Dimension(600, 390));

        JPanel lookHead = new JPanel(new BorderLayout());
        lookHead.setOpaque(false);
        lookHead.add(Widgets.caption("ВНЕШНИЙ ВИД", Palette.SURFACE), BorderLayout.WEST);
        JPanel lookChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        lookChips.setOpaque(false);
        lookChips.add(new Widgets.Chip("QuickSkin", Palette.ACCENT));
        lookChips.add(new Widgets.Chip("PNG · GIF", Palette.MUTED));
        lookHead.add(lookChips, BorderLayout.EAST);
        skinStatus.setText("Потяни модель мышью — она повернётся");
        JPanel lookTop = new JPanel();
        lookTop.setOpaque(false);
        lookTop.setLayout(new BoxLayout(lookTop, BoxLayout.Y_AXIS));
        lookTop.add(lookHead);
        lookTop.add(Box.createVerticalStrut(6));
        lookTop.add(skinStatus);
        lookCard.add(lookTop, BorderLayout.NORTH);

        preview = new SkinView();
        preview.setPreferredSize(new Dimension(240, 260));
        preview.setMinimumSize(new Dimension(200, 230));
        preview.setMaximumSize(new Dimension(240, 260));
        preview.setBorder(BorderFactory.createEmptyBorder());
        lookCard.add(preview, BorderLayout.WEST);

        JPanel slots = new JPanel();
        slots.setOpaque(false);
        slots.setLayout(new BoxLayout(slots, BoxLayout.Y_AXIS));

        slots.add(Widgets.caption("СКИН", Palette.SURFACE));
        slots.add(Box.createVerticalStrut(4));
        slots.add(left(skinFile));
        slots.add(Box.createVerticalStrut(2));
        slots.add(left(skinMeta));
        slots.add(Box.createVerticalStrut(8));
        JPanel skinButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        skinButtons.setOpaque(false);
        skinButtons.setAlignmentX(LEFT_ALIGNMENT);
        skinButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        Widgets.BerryButton pickSkin = new Widgets.BerryButton("Выбрать файл", Widgets.BerryButton.Kind.SECONDARY);
        pickSkin.addActionListener(e -> choose(false));
        applySkin = new Widgets.BerryButton("Применить скин", Widgets.BerryButton.Kind.PRIMARY);
        applySkin.setEnabled(false);
        applySkin.addActionListener(e -> apply(false));
        skinButtons.add(pickSkin);
        skinButtons.add(applySkin);
        slots.add(skinButtons);

        slots.add(Box.createVerticalStrut(14));
        JSeparator line = new JSeparator();
        line.setForeground(Palette.LINE);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        slots.add(line);
        slots.add(Box.createVerticalStrut(14));

        slots.add(Widgets.caption("ПЛАЩ", Palette.SURFACE));
        slots.add(Box.createVerticalStrut(4));
        slots.add(left(capeFile));
        slots.add(Box.createVerticalStrut(2));
        slots.add(left(capeMeta));
        slots.add(Box.createVerticalStrut(8));
        JPanel capeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        capeButtons.setOpaque(false);
        capeButtons.setAlignmentX(LEFT_ALIGNMENT);
        capeButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        Widgets.BerryButton pickCape = new Widgets.BerryButton("Выбрать файл", Widgets.BerryButton.Kind.SECONDARY);
        pickCape.addActionListener(e -> choose(true));
        applyCape = new Widgets.BerryButton("Применить плащ", Widgets.BerryButton.Kind.PRIMARY);
        applyCape.setEnabled(false);
        applyCape.addActionListener(e -> apply(true));
        clearCape = new Widgets.BerryButton("Снять", Widgets.BerryButton.Kind.GHOST);
        clearCape.addActionListener(e -> removeCape());
        capeButtons.add(pickCape);
        capeButtons.add(applyCape);
        capeButtons.add(clearCape);
        slots.add(capeButtons);

        slots.add(Box.createVerticalStrut(10));
        JTextArea note = Widgets.paragraph(
                "Файлы кладутся прямо в пул QuickSkin: скин — в quickskin/uploads/skins, "
                        + "плащ — в quickskin/uploads/capes. Перетащи PNG или GIF в карточку — "
                        + "лаунчер сам поймёт, скин это или плащ.",
                Palette.reg(8.6f), Palette.DIM);
        note.setAlignmentX(LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        slots.add(note);

        lookCard.add(slots, BorderLayout.CENTER);



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

        add(lookCard);
        add(Box.createVerticalGlue());

        loadExisting();
        refresh();

        // токен пишет игра после входа — лаунчер сам подхватит новое состояние
        javax.swing.Timer poll = new javax.swing.Timer(2500, event -> {
            int state = Auth.tokenState(host.prism());
            if (state == lastState) {
                return;
            }
            refresh();
            if (state == Auth.TOKEN_OK) {
                host.setState("Вход подтверждён — пустим в игру", Palette.OK);
            }
        });
        poll.start();
    }

    private JLabel left(JLabel label) {
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    /** Показывает последний скин и плащ из пула мода. */
    private void loadExisting() {
        Skins.Asset skin = Skins.currentAsset(host.prism(), false);
        Skins.Asset cape = Skins.currentAsset(host.prism(), true);
        preview.setSkin(skin == null ? null : skin.image);
        preview.setCape(cape == null ? null : cape.image);
        updateSkinLabels();
        updateCapeLabels();
    }

    public void refresh() {
        int state = Auth.tokenState(host.prism());
        lastState = state;
        String nick = Auth.tokenNick(host.prism());
        if (nick.isEmpty()) nick = Auth.nick();
        if (state == Auth.TOKEN_OK) {
            status.setText("Вход выполнен" + (nick.isEmpty() ? "" : " · " + nick));
            status.setForeground(Palette.OK);
            hint.setText("Вход подтверждён: токен в сборке, сервер пустит сразу.");
        } else {
            status.setText(state == Auth.TOKEN_EXPIRED ? "Сессия истекла — войди снова"
                    : state == Auth.TOKEN_OTHER_NICK ? "Токен выдан другому нику"
                    : state == Auth.TOKEN_BROKEN ? "Токен повреждён — войди снова"
                    : "Вход не выполнен");
            status.setForeground(Palette.WARN);
            hint.setText(state == Auth.TOKEN_NONE
                    ? "Нажми «Войти» — откроется бот, нажми Start и запускай игру."
                    : "Нажми «Войти» ещё раз: сохранённый токен больше не подходит.");
        }
        boolean signed = state == Auth.TOKEN_OK;
        login.setText(signed ? "Войти заново" : "Войти через Telegram");
        logout.setVisible(signed);
        remember.setSelected(Store.remember());
        if (nickField.getText().trim().isEmpty()) {
            String name = Auth.nick();
            if (name.isEmpty() && host.prism() != null) name = host.prism().accountName();
            nickField.setText(name);
        }
        revalidate();
        repaint();
    }

    private String tokenMessage(int state) {
        if (state == Auth.TOKEN_OK) return "Вход подтверждён";
        if (state == Auth.TOKEN_EXPIRED) return "Токен истёк — войди снова";
        if (state == Auth.TOKEN_OTHER_NICK) return "Токен выдан другому нику";
        if (state == Auth.TOKEN_BROKEN) return "Файл токена повреждён";
        return "Токена пока нет";
    }

    private void startLogin() {
        String nick = nickField.getText().trim();
        if (nick.isEmpty()) {
            status.setText("Напиши ник из Minecraft");
            status.setForeground(Palette.WARN);
            return;
        }
        String error = Auth.start(host.prism(), nick);
        if (!error.isEmpty()) {
            status.setText(error);
            status.setForeground(Palette.WARN);
            return;
        }
        status.setText("Открыл бота в браузере");
        status.setForeground(Palette.ACCENT);
        hint.setText("Ссылка выдана для «" + nick + "». Нажми Start в боте — и запускай игру, "
                + "сервер пустит автоматически.");
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
            host.setState(error.getMessage(), Palette.ERR);
        }
    }

    private void showSkin(Skins.Asset asset) {
        pickedSkin = asset;
        preview.setSkin(asset.image);
        updateSkinLabels();
        skinStatus.setText("Скин примерен: " + asset.sizeText() + " · нажми «Применить скин»");
        skinStatus.setForeground(Palette.MUTED);
        host.setState("Скин примерен", Palette.MUTED);
    }

    private void showCape(Skins.Asset asset) {
        pickedCape = asset;
        preview.setCape(asset.image);
        updateCapeLabels();
        skinStatus.setText("Плащ примерен: " + asset.sizeText() + " · нажми «Применить плащ»");
        skinStatus.setForeground(Palette.MUTED);
        host.setState("Плащ примерен", Palette.MUTED);
    }

    private void updateSkinLabels() {
        if (pickedSkin == null) {
            skinFile.setText("Скин не выбран");
            skinMeta.setText("PNG или GIF · 64×32, 64×64 … 2048×2048");
            applySkin.setEnabled(false);
        } else {
            skinFile.setText(pickedSkin.file.getName());
            skinMeta.setText("примерен · " + pickedSkin.sizeText());
            applySkin.setEnabled(true);
        }
    }

    private void updateCapeLabels() {
        if (pickedCape == null) {
            capeFile.setText("Плащ не выбран");
            capeMeta.setText("PNG или GIF · 64×32 … 2048×1024");
            applyCape.setEnabled(false);
        } else {
            capeFile.setText(pickedCape.file.getName());
            capeMeta.setText("примерен · " + pickedCape.sizeText());
            applyCape.setEnabled(true);
        }
    }

    private void apply(boolean cape) {
        Skins.Asset asset = cape ? pickedCape : pickedSkin;
        if (asset == null) return;
        if (cape) applyCape.setEnabled(false); else applySkin.setEnabled(false);
        skinStatus.setText("Копирую в пул QuickSkin…");
        skinStatus.setForeground(Palette.MUTED);
        new Thread(() -> {
            String message;
            boolean ok = true;
            try {
                String name = cape
                        ? Skins.applyCape(host.prism(), asset.file)
                        : Skins.applySkin(host.prism(), asset.file, Auth.nick());
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
                skinStatus.setText(text + (success ? " · выбери его в меню QuickSkin, если не применился" : ""));
                skinStatus.setForeground(success ? Palette.OK : Palette.ERR);
                host.setState(text, success ? Palette.OK : Palette.ERR);
            });
        }, "berry-skin").start();
    }

    private void removeCape() {
        Skins.clearCape(host.prism());
        pickedCape = null;
        preview.setCape(null);
        updateCapeLabels();
        skinStatus.setText("Плащ снят — в QuickSkin выбран вариант «без плаща»");
        skinStatus.setForeground(Palette.MUTED);
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
}
