package id.satria.launcher.utils

import java.util.Calendar

fun getAssistantMessage(userName: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour >= 22 || hour < 4  -> "It's late, $userName. Put the phone down and get some rest! 😠 Your health comes first."
        hour in 4..10           -> "Good morning, $userName! ☀️ Rise and conquer the day. I'm always cheering for you! 😘"
        hour in 11..14          -> "Take a break! 😊 Have you had lunch yet, $userName? Don't skip meals! 🍔"
        hour in 15..17          -> "You must be tired, $userName. ☕ Go ahead and take a breather, okay? 🤗"
        else                    -> "All done for the day? 🌙 Time to wind down and relax, $userName. You deserve it. 🥰"
    }
}
