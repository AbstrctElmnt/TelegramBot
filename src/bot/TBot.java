package bot;

import bot.bittrex.Bittrex;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

public class TBot extends TelegramLongPollingBot {
    private Bittrex bittrex;
    private TBot bot;
    private String chatId;

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new TBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getBotUsername() {
        return "dhsghe_bot"; // AE Bot ""ИМЯ_ПОЛЬЗОВАТЕЛЯ_ВАШЕГО_БОТА";
    }

    @Override
    public String getBotToken() {
        return "422255629:AAE3hK-LInqbWRT2UHJqt5T4bRX58YL8o4I";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {

            if (message.getText().equals("/help")) {
                sendMsg(message,
                        "/check-MarketName - returns all info about specified coin" + "\n" +
                        "/bh-start - go with harvester on Bittrex" + "\n" +
                        "/bh-stop - stop harvester");
            }
            else if(message.getText().contains("/check-")) {
                if (bittrex == null) bittrex = new Bittrex(this);
                String coin = message.getText();
                coin = coin.substring(coin.indexOf("-")+1);
                sendMsg(message,bittrex.getMarketSummary(coin));
            }
            else if(message.getText().equals("/bh-start"))
            {
                if (bittrex == null) bittrex = new Bittrex(this);
                sendMsg(message,"Harvester is active");

            }
            else if(message.getText().equals("/bh-stop"))
            {
                bittrex.setActive(false);
                bittrex = null;
                sendMsg(message,"Harvester is not active");
            }
            else
                sendMsg(message, "Refer to /help");
        }
    }

    public void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        chatId = message.getChatId().toString();
        //sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);

        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        //sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);

        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
