package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {
    val allIcons =
        listOf(
                // Household chores and hobbies
                "Dishwasher",
                "LocalLaundryService",
                "Kitchen",
                "Countertops",
                "Soap",
                "Iron",
                "Checkroom",
                "TableRestaurant",
                "Flatware",
                "OutdoorGrill",
                "WaterDrop",
                "Shower",
                "Recycling",
                "ShoppingBasket",
                "Medication",
                "Piano",
                "Headphones",
                "Palette",
                "Museum",
                "LocalFlorist",
                "Forest",
                "Hiking",
                "Skateboarding",
                "Savings",
                // General
                "Add",
                "Check",
                "Close",
                "Delete",
                "Edit",
                "Home",
                "Settings",
                "Star",
                "Info",
                "Warning",
                "Search",
                "Share",
                "Favorite",
                "Visibility",
                "Lock",
                "LockOpen",
                "Menu",
                "MoreVert",
                "ExitToApp",
                "Logout",
                "Help",
                "AccountCircle",
                "Face",
                "Person",
                "Group",
                "Notifications",
                "SentimentSatisfied",
                "Language",

                // Actions
                "ThumbUp",
                "Email",
                "Phone",
                "Send",
                "Drafts",
                "Archive",
                "Inbox",
                "Outbox",
                "Cloud",
                "Refresh",
                "Loop",
                "Repeat",
                "History",
                "Schedule",
                "Timer",
                "Alarm",
                "Event",
                "CalendarToday",

                // Entertainment & Media
                "MusicNote",
                "Games",
                "Movie",
                "Tv",
                "Mic",
                "Camera",
                "PlayArrow",
                "Pause",
                "Stop",
                "SkipNext",
                "SkipPrevious",
                "VolumeUp",
                "VolumeDown",
                "BrightnessLow",
                "BrightnessHigh",
                "Redeem",
                "EmojiEvents",
                "Whatshot",
                "Celebration",

                // Food & Life
                "Fastfood",
                "Restaurant",
                "LocalCafe",
                "LocalPizza",
                "LocalBar",
                "Cake",
                "Icecream",
                "Pets",
                "SportsBasketball",
                "DirectionsRun",
                "DirectionsBike",
                "FitnessCenter",
                "Pool",
                "Spa",
                "BeachAccess",
                "Umbrella",
                "Yard",
                "NightsStay",
                "CleanHands",
                "VolunteerActivism",
                "Bed",
                "CleaningServices",
                "Bathtub",
                "Wash",
                "ChildCare",
                "Stroller",
                "SmartToy",
                "SportsRugby",
                "SportsFootball",
                "SportsSoccer",
                "SportsBaseball",
                "SportsTennis",
                "SportsVolleyball",

                // Education & Work
                "Book",
                "MenuBook",
                "School",
                "Work",
                "Assessment",
                "Dashboard",
                "Extension",
                "Lightbulb",
                "Science",
                "Psychology",
                "SelfImprovement",
                "Brush",
                "Terrain",
                "Public",
                "Place",
                "AutoStories",
                "RocketLaunch",
                "AutoFixHigh",
                "Spellcheck",
                "Calculate",
                "Draw",

                // Devices
                "Laptop",
                "Smartphone",
                "Tablet",
                "Watch",
                "GridView",
                "List",
                "FilterList",
                "Sort",

                // Outlined versions
                "AddOutlined",
                "StarOutlined",
                "FavoriteOutlined",
                "NotificationsOutlined",
                "LightbulbOutlined",
                "EditOutlined",
                "DeleteOutlined",
                "InfoOutlined",
                "HelpOutlined",
                "SettingsOutlined",

                // AutoMirrored
                "ArrowBack",
                "ArrowForward",
                "KeyboardArrowLeft",
                "KeyboardArrowRight",
                "ListAlt",
                "Notes",
            )
            .sorted()

    // https://github.com/composablehorizons/compose-icons

    fun getIconByName(name: String?): ImageVector {
        return try {
            when (name) {
                "Dishwasher" -> DishwasherIcon
                "LocalLaundryService" -> Icons.Default.LocalLaundryService
                "Kitchen" -> Icons.Default.Kitchen
                "Countertops" -> Icons.Default.Countertops
                "Soap" -> Icons.Default.Soap
                "Iron" -> Icons.Default.Iron
                "Checkroom" -> Icons.Default.Checkroom
                "TableRestaurant" -> Icons.Default.TableRestaurant
                "Flatware" -> Icons.Default.Flatware
                "OutdoorGrill" -> Icons.Default.OutdoorGrill
                "WaterDrop" -> Icons.Default.WaterDrop
                "Shower" -> Icons.Default.Shower
                "Recycling" -> Icons.Default.Recycling
                "ShoppingBasket" -> Icons.Default.ShoppingBasket
                "Medication" -> Icons.Default.Medication
                "Piano" -> Icons.Default.Piano
                "Headphones" -> Icons.Default.Headphones
                "Palette" -> Icons.Default.Palette
                "Museum" -> Icons.Default.Museum
                "LocalFlorist" -> Icons.Default.LocalFlorist
                "Forest" -> Icons.Default.Forest
                "Hiking" -> Icons.Default.Hiking
                "Skateboarding" -> Icons.Default.Skateboarding
                "Savings" -> Icons.Default.Savings

                "Add" -> Icons.Default.Add
                "Check" -> Icons.Default.Check
                "Close" -> Icons.Default.Close
                "Delete" -> Icons.Default.Delete
                "Edit" -> Icons.Default.Edit
                "Home" -> Icons.Default.Home
                "Settings" -> Icons.Default.Settings
                "Star" -> Icons.Default.Star
                "Info" -> Icons.Default.Info
                "Warning" -> Icons.Default.Warning
                "Search" -> Icons.Default.Search
                "Share" -> Icons.Default.Share
                "Favorite" -> Icons.Default.Favorite
                "Visibility" -> Icons.Default.Visibility
                "Lock" -> Icons.Default.Lock
                "LockOpen" -> Icons.Default.LockOpen
                "Menu" -> Icons.Default.Menu
                "MoreVert" -> Icons.Default.MoreVert
                "ExitToApp" -> Icons.Default.ExitToApp
                "Logout" -> Icons.Default.Logout
                "Help" -> Icons.Default.Help
                "AccountCircle" -> Icons.Default.AccountCircle
                "Face" -> Icons.Default.Face
                "Person" -> Icons.Default.Person
                "Group" -> Icons.Default.Group
                "Notifications" -> Icons.Default.Notifications
                "SentimentSatisfied" -> Icons.Default.SentimentSatisfied
                "Language" -> Icons.Default.Language
                "ThumbUp" -> Icons.Default.ThumbUp
                "Email" -> Icons.Default.Email
                "Phone" -> Icons.Default.Phone
                "Send" -> Icons.Default.Send
                "Drafts" -> Icons.Default.Drafts
                "Archive" -> Icons.Default.Archive
                "Inbox" -> Icons.Default.Inbox
                "Outbox" -> Icons.Default.Outbox
                "Cloud" -> Icons.Default.Cloud
                "Refresh" -> Icons.Default.Refresh
                "Loop" -> Icons.Default.Loop
                "Repeat" -> Icons.Default.Repeat
                "History" -> Icons.Default.History
                "Schedule" -> Icons.Default.Schedule
                "Timer" -> Icons.Default.Timer
                "Alarm" -> Icons.Default.Alarm
                "Event" -> Icons.Default.Event
                "CalendarToday" -> Icons.Default.CalendarToday
                "MusicNote" -> Icons.Default.MusicNote
                "Games" -> Icons.Default.Games
                "Movie" -> Icons.Default.Movie
                "Tv" -> Icons.Default.Tv
                "Mic" -> Icons.Default.Mic
                "Camera" -> Icons.Default.Camera
                "PlayArrow" -> Icons.Default.PlayArrow
                "Pause" -> Icons.Default.Pause
                "Stop" -> Icons.Default.Stop
                "SkipNext" -> Icons.Default.SkipNext
                "SkipPrevious" -> Icons.Default.SkipPrevious
                "VolumeUp" -> Icons.Default.VolumeUp
                "VolumeDown" -> Icons.Default.VolumeDown
                "BrightnessLow" -> Icons.Default.BrightnessLow
                "BrightnessHigh" -> Icons.Default.BrightnessHigh
                "Redeem" -> Icons.Default.Redeem
                "EmojiEvents" -> Icons.Default.EmojiEvents
                "Whatshot" -> Icons.Default.Whatshot
                "Celebration" -> Icons.Default.Celebration
                "Fastfood" -> Icons.Default.Fastfood
                "Restaurant" -> Icons.Default.Restaurant
                "LocalCafe" -> Icons.Default.LocalCafe
                "LocalPizza" -> Icons.Default.LocalPizza
                "LocalBar" -> Icons.Default.LocalBar
                "Cake" -> Icons.Default.Cake
                "Icecream" -> Icons.Default.Icecream
                "Pets" -> Icons.Default.Pets
                "SportsBasketball" -> Icons.Default.SportsBasketball
                "DirectionsRun" -> Icons.Default.DirectionsRun
                "DirectionsBike" -> Icons.Default.DirectionsBike
                "FitnessCenter" -> Icons.Default.FitnessCenter
                "Pool" -> Icons.Default.Pool
                "Spa" -> Icons.Default.Spa
                "BeachAccess" -> Icons.Default.BeachAccess
                "Umbrella" -> Icons.Default.Umbrella
                "Yard" -> Icons.Default.Yard
                "NightsStay" -> Icons.Default.NightsStay
                "CleanHands" -> Icons.Default.CleanHands
                "VolunteerActivism" -> Icons.Default.VolunteerActivism
                "Book" -> Icons.Default.Book
                "MenuBook" -> Icons.Default.MenuBook
                "School" -> Icons.Default.School
                "Work" -> Icons.Default.Work
                "Assessment" -> Icons.Default.Assessment
                "Dashboard" -> Icons.Default.Dashboard
                "Extension" -> Icons.Default.Extension
                "Lightbulb" -> Icons.Default.Lightbulb
                "Science" -> Icons.Default.Science
                "Psychology" -> Icons.Default.Psychology
                "SelfImprovement" -> Icons.Default.SelfImprovement
                "Brush" -> Icons.Default.Brush
                "Terrain" -> Icons.Default.Terrain
                "Public" -> Icons.Default.Public
                "Place" -> Icons.Default.Place
                "Laptop" -> Icons.Default.Laptop
                "Smartphone" -> Icons.Default.Smartphone
                "Tablet" -> Icons.Default.Tablet
                "Watch" -> Icons.Default.Watch
                "GridView" -> Icons.Default.GridView
                "List" -> Icons.Default.List
                "FilterList" -> Icons.Default.FilterList
                "Sort" -> Icons.Default.Sort

                "CleaningServices" -> Icons.Default.CleaningServices
                "Bathtub" -> Icons.Default.Bathtub
                "Wash" -> Icons.Default.Wash
                "ChildCare" -> Icons.Default.ChildCare
                "Stroller" -> Icons.Default.Stroller
                "SmartToy" -> Icons.Default.SmartToy
                "SportsRugby" -> Icons.Default.SportsRugby
                "SportsFootball" -> Icons.Default.SportsFootball
                "SportsSoccer" -> Icons.Default.SportsSoccer
                "SportsBaseball" -> Icons.Default.SportsBaseball
                "SportsTennis" -> Icons.Default.SportsTennis
                "SportsVolleyball" -> Icons.Default.SportsVolleyball
                "AutoStories" -> Icons.Default.AutoStories
                "RocketLaunch" -> Icons.Default.RocketLaunch
                "AutoFixHigh" -> Icons.Default.AutoFixHigh
                "Spellcheck" -> Icons.Default.Spellcheck
                "Calculate" -> Icons.Default.Calculate
                "Draw" -> Icons.Default.Draw
                "Bed" -> Icons.Default.Bed

                // Outlined
                "AddOutlined" -> Icons.Outlined.Add
                "StarOutlined" -> Icons.Outlined.Star
                "FavoriteOutlined" -> Icons.Outlined.Favorite
                "NotificationsOutlined" -> Icons.Outlined.Notifications
                "LightbulbOutlined" -> Icons.Outlined.Lightbulb
                "EditOutlined" -> Icons.Outlined.Edit
                "DeleteOutlined" -> Icons.Outlined.Delete
                "InfoOutlined" -> Icons.Outlined.Info
                "HelpOutlined" -> Icons.Outlined.Help
                "SettingsOutlined" -> Icons.Outlined.Settings

                // AutoMirrored
                "ArrowBack" -> Icons.AutoMirrored.Filled.ArrowBack
                "ArrowForward" -> Icons.AutoMirrored.Filled.ArrowForward
                "KeyboardArrowLeft" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
                "KeyboardArrowRight" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
                "ListAlt" -> Icons.AutoMirrored.Filled.ListAlt
                "Notes" -> Icons.AutoMirrored.Filled.Notes

                else -> Icons.Default.Check
            }
        } catch (e: Exception) {
            Icons.Default.Check
        }
    }

    fun getEmojiForIcon(name: String): String {
        return "✨"
    }
}
