package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TelegramBot telegramBot;

    private final NotificationTaskRepository notificationTaskRepository;

    @Autowired
    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(this::processUpdate);

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        logger.info("Processing update: {}", update);

        // Текст сообщения из чата
        final String text = update.message().text();

        // ID чата
        final Long chatId = update.message().chat().id();

        // Если в чат написали /start, отправляем в ответ приветственное сообщение
        if ("/start".equals(text)) {
            sendWelcomeMessage(chatId);
            return;
        }

        // Разбиваем пришедшее сообщение на дату и текст
        final Matcher matcher = Pattern
                .compile("([\\d\\.\\:\\s]{16})(\\s)([\\W+]+)")
                .matcher(text);

        // Если текст сообщения не соответствует, отправляем сообщение о некорректности введенных данных
        if (!matcher.matches()) {
            sendErrorMessage(chatId);
            return;
        }

        // Дата и время уведомления
        final String notificationDateTimeStr = matcher.group(1);

        // Текст уведомления
        final String notificationText = matcher.group(3);

        // Паттерн не учитывает все случаи, поэтому может возникнуть ошибка при парсинге строки в LocalDateTime
        LocalDateTime notificationDateTime;
        try {
            notificationDateTime = LocalDateTime.parse(notificationDateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            sendErrorMessage(chatId);
            return;
        }

        //Создаем уведомление
        NotificationTask notificationTask = new NotificationTask(chatId, notificationText, notificationDateTime);

        //Добавляем уведомление в БД
        notificationTask = notificationTaskRepository.save(notificationTask);

        //Генерация сообщения уведомления
        sendMessage(chatId, "Напоминание \"%s\" я отправлю вам %s".formatted(
                notificationTask.getText(),
                notificationTask.getDateTime().format(DATE_TIME_FORMATTER))
        );
    }

    private void sendErrorMessage(Long chatId) {
        final String errorMessage = """
                    Введено некорректное сообщение!
                    Пожалуйста, введите сообщение в следующем формате:
                    "%s Текст Вашего напоминания"
                """.formatted(LocalDateTime.now().plusMinutes(2).format(DATE_TIME_FORMATTER));
        sendMessage(chatId, errorMessage);
    }

    private void sendWelcomeMessage(Long chatId) {
        final String welcomeText = """
        Привет! Я умею присылать уведомления в назначенное тобой время!
        Пожалуйста, введите сообщение в следующем формате:
        "%s Текст Вашего напоминания"
        """.formatted(LocalDateTime.now().plusMinutes(2).format(DATE_TIME_FORMATTER));
        sendMessage(chatId, welcomeText);
    }

    private void sendMessage(Long chatId, String text) {
        telegramBot.execute(new SendMessage(chatId, text));
    }
}
