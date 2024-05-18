Overview
This project presents a convenient Android client app for Home Assistant, a popular open-source platform for home automation. Home Assistant can be deployed on various environments including local machines, virtual machines, Docker containers, Raspberry Pi, and more. For further information, please visit the Home Assistant website. Quick installation instructions for Windows can be found in the HassWP repository.

Objective
The primary goal of this project is to create a more user-friendly interface for beginners using the Home Assistant companion app on Android. The default companion app can be challenging for users with limited knowledge of network concepts such as IP addresses or DNS. Additionally, setting up automations through the companion app often involves navigating through the server's web interface, which can be confusing without proper guidance.

This app aims to address these issues by focusing on the following areas:

Auto-Discovery of Servers:

Utilizes the Zeroconf protocol to automatically discover Home Assistant servers on the local network.
The companion app sometimes struggles to find servers even when they are up and running. This app aims to improve the reliability and speed of server discovery.
Simplified Login Process:

Uses the web UI for the login process via a web view to ensure a seamless and familiar experience for users.
Basic Lighting Services and Automation:

Provides a straightforward interface to control basic lighting services such as turning lights on and off.
Allows users to create event-based lighting automations, such as turning on the lights 30 minutes before sunrise.
Features
Server Auto-Discovery: Quickly find and connect to Home Assistant servers on your local network without manual configuration.
Intuitive Login: Streamlined login process using the existing web UI.
User-Friendly Lighting Control: Easily manage lighting settings and automations through a simplified interface.
Event-Based Automations: Set up lighting automations based on specific events, like sunrise, without needing to navigate complex menus.
This app is designed to make home automation accessible and straightforward for users at all technical levels.






