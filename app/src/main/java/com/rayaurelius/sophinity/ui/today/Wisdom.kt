package com.rayaurelius.sophinity.ui.today

import java.time.LocalDate

/**
 * A line of ancient Greek wisdom for each day. Only well-attested sayings, with the source,
 * in plain modern English. (Several famous "Greek" quotes are misattributed; e.g. "We are what
 * we repeatedly do" is Will Durant summarizing Aristotle, so it is not included.)
 */
data class Wisdom(val text: String, val who: String, val source: String)

object Wisdoms {
    val all = listOf(
        Wisdom("I grow old ever learning many things.", "Solon", "fragment, quoted by Plato and Plutarch"),
        Wisdom("All people by nature desire to know.", "Aristotle", "Metaphysics, Book I"),
        Wisdom("The unexamined life is not worth living.", "Socrates", "Plato, Apology"),
        Wisdom("Wonder is the beginning of wisdom.", "Socrates", "Plato, Theaetetus"),
        Wisdom("Know thyself.", "Inscription at Delphi", "Temple of Apollo"),
        Wisdom("What we have to learn to do, we learn by doing.", "Aristotle", "Nicomachean Ethics, Book II"),
        Wisdom("It is impossible to learn what you think you already know.", "Epictetus", "Discourses, Book II"),
        Wisdom("No great thing is created suddenly, any more than a bunch of grapes or a fig.", "Epictetus", "Discourses, Book I"),
        Wisdom("First say to yourself what you would be; then do what you have to do.", "Epictetus", "Discourses, Book III"),
        Wisdom("The roots of education are bitter, but the fruit is sweet.", "Aristotle", "as quoted by Diogenes Laërtius"),
        Wisdom("Education is the best provision for old age.", "Aristotle", "as quoted by Diogenes Laërtius"),
        Wisdom("The mind is not a vessel to be filled, but wood to be kindled.", "Plutarch", "On Listening to Lectures"),
        Wisdom("The beginning is half of the whole.", "Aristotle", "Politics, Book V"),
        Wisdom("Practice is everything.", "Periander", "as quoted by Diogenes Laërtius"),
        Wisdom("We have two ears and one mouth, so we may listen more and speak less.", "Zeno of Citium", "as quoted by Diogenes Laërtius"),
        Wisdom("As are your habitual thoughts, so will be the character of your mind.", "Marcus Aurelius", "Meditations, Book V"),
        Wisdom("Waste no more time arguing what a good person should be. Be one.", "Marcus Aurelius", "Meditations, Book X"),
        Wisdom("You cannot step into the same river twice.", "Heraclitus", "as reported by Plato, Cratylus"),
    )

    fun forDay(day: LocalDate): Wisdom = all[(day.toEpochDay() % all.size).toInt()]
}
