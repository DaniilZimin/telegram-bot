package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationTaskScheduler {

    private final TelegramBot telegramBot;

    private final NotificationTaskRepository repository;

    @Autowired
    public NotificationTaskScheduler(TelegramBot telegramBot, NotificationTaskRepository repository) {
        this.telegramBot = telegramBot;
        this.repository = repository;
    }

    //Метод каждую минуту проверяет есть ли подходящие уведомление для данного времени
    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotifications() {
        final LocalDateTime nowDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        final List<NotificationTask> notificationTasks = repository.findAllByDateTime(nowDateTime);

        notificationTasks.forEach(this::sendNotification);
    }

    //Метод отправляет уведомление в чат и если оно успешно добавилось то удаляет его из БД
    private void sendNotification(NotificationTask task) {
        final SendResponse response = telegramBot.execute(new SendMessage(task.getChatId(), task.getText()));

        if (response.isOk()) {
            repository.delete(task);
        }
    }
}
