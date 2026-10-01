using System;

namespace RhinoS
{
    public class DeathDate
    {
        static readonly Random Rng = new Random();

        static DateTime GetBirthday()
        {
            int month = ReadInt("Month [1-12]]: ", 1, 12);
            int year = ReadYear("Year: ");
            int day = ReadDay(month, year);
            return new DateTime(year, month, day);
        }

        static void PrintHeader()
        {
            Console.WriteLine("========================================");
            Console.WriteLine(" ");
            Console.WriteLine("========================================");
            Console.WriteLine();
        }

        static void ShowBirthdayInfo(DateTime birthday)
        {
            DateTime today = DateTime.Today;

            Console.WriteLine();
            Console.WriteLine($"  Your birthday: {birthday:MMMM d, yyyy}");

            if (birthday <= today)
            {
                int age = CalculateAge(birthday, today);
                Console.WriteLine($"  Age: {age} year{(age == 1 ? "" : "s")} old");
                ShowNextBirthday(birthday, today);
            }
            else
            {
                int daysUntil = (birthday - today).Days;
                Console.WriteLine($"  Arrives in: {daysUntil} day{(daysUntil == 1 ? "" : "s")} from today");
            }
        }

        static void ShowNextBirthday(DateTime birthday, DateTime today)
        {
            DateTime next = NextBirthdayFrom(birthday, today);
            int daysLeft = (next - today).Days;

            if (daysLeft == 0)
            {
                Console.WriteLine("  Today is your birthday!");
            }
            else
            {
                Console.WriteLine($"  Next birthday: {next:MMMM d, yyyy} ({daysLeft} day{(daysLeft == 1 ? "" : "s")} away)");
            }
        }

        static void ShowFate(DateTime birthday)
        {
            Console.WriteLine();
            Console.Write("  Press Enter...\n");
            Console.ReadLine();

            DateTime today = DateTime.Today;
            DateTime startDate = birthday > today ? birthday : today;
            DateTime fateDate = GenerateFutureDate(startDate, startDate.AddYears(50));

            Console.WriteLine($"  Death date: {fateDate:MMMM d, yyyy}");
            Console.WriteLine();
        }

        static bool AskPlayAgain()
        {
            Console.Write("  Play again? (y/n): ");
            string answer = (Console.ReadLine() ?? "").Trim().ToLower();
            return answer == "y" || answer == "yes";
        }


        static int ReadInt(string prompt, int min, int max)
        {
            while (true)
            {
                Console.Write($"  {prompt}");
                string input = Console.ReadLine() ?? "";
                
                //C
                if (int.TryParse(input.Trim(), out int value) && value >= min && value <= max)
                return value;
                //C 

                Console.WriteLine($"  Invalid. Enter a number between {min} and {max}.\n");
            }
        }

        static int ReadYear(string prompt)
        {
            int currentYear = DateTime.Today.Year;
            while (true)
            {
                Console.Write($"  {prompt}");
                string input = (Console.ReadLine() ?? "").Trim();

                //C
                if (input.Length == 4 && int.TryParse(input, out int year) && year >= 1900 && year <= currentYear)
                return year;
                //C

                Console.WriteLine($"  Invalid Input.\n");
            }
        }

        static int ReadDay(int month, int year)
        {
            int maxDay = DateTime.DaysInMonth(year, month);
            
            while (true)
            {
                Console.Write($"  Day (1-{maxDay}): ");
                string input = (Console.ReadLine() ?? "").Trim();

                //C
                if (int.TryParse(input, out int day) && day >= 1 && day <= maxDay)
                return day;
                //C

                Console.WriteLine($"  Invalid Input.\n");
            }
        }


        static DateTime GenerateFutureDate(DateTime start, DateTime end)
        {
            
            if (start >= end)
            {
                return start;
            }
            
            int range = (end - start).Days;
            return start.AddDays(Rng.Next(0, range + 1));
        }

        static int CalculateAge(DateTime birthday, DateTime today)
        {
            
            int age = today.Year - birthday.Year;
            if (today < birthday.AddYears(age)) age--;
            return age;
        }

        static DateTime NextBirthdayFrom(DateTime birthday, DateTime today)
        {
            
            DateTime next = new DateTime(today.Year, birthday.Month, birthday.Day);
            
            if (next < today) next = next.AddYears(1);
            {
                return next;
            }
        }
        
        public static void Main(string[] args)
        {
            while (true)
            {
                Console.Clear();
                PrintHeader();

                DateTime birthday = GetBirthday();
                ShowBirthdayInfo(birthday);
                ShowFate(birthday);

                if(!AskPlayAgain())
                {
                    break;
                }
            }

            Console.WriteLine("\nProgram Ended.\n");
        }
    }
}
