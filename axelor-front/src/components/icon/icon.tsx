import {
  BootstrapIcon,
  BootstrapIconName,
  bootstrapIconNames,
} from "@axelor/ui/icons/bootstrap-icon";

import {
  MaterialIcon,
  MaterialIconProps,
  materialIconNames,
} from "@axelor/ui/icons/material-icon";
import { forwardRef } from "react";

/**
 * Map removed material icons with their equivalency to maintained compatibility
 * See https://material-symbols-changelog.vercel.app/
 */
const materialIconsRemoved: Record<string, MaterialIconProps["icon"]> = {
  // 2024-05-14
  arrow_back_ios_new: "arrow_back_ios",
  done: "check",
  expand_less: "keyboard_arrow_up",
  expand_more: "keyboard_arrow_down",
  file_download_done: "download_done",
  navigate_before: "chevron_left",
  navigate_next: "chevron_right",
  // 2024-05-30
  cut: "content_cut",
  emoji_flags: "flag",
  feed: "description",
  monetization_on: "paid",
  // 2024-06-06
  wifi_calling_1: "wifi_calling_bar_3",
  wifi_calling_2: "wifi_calling_bar_2",
  wifi_calling_3: "wifi_calling_bar_3",
  // 2024-07-02
  airplanemode_active: "flight",
  clear_night: "bedtime",
  device_reset: "history",
  flightsmode: "travel",
  lens: "circle",
  panorama_fish_eye: "circle",
  quiet_time: "bedtime",
  quiet_time_active: "bedtime_off",
  // 2024-08-08
  restaurant_menu: "restaurant",
  // 2024-09-06
  ev_charger: "ev_station",
  // 2024-10-01
  pixel_9_pro_fold: "devices_fold",
  reg_logo_ift: "communication",
  // 2024-10-11
  width: "arrow_range",
  // 2024-11-02
  grade: "star",
  // 2024-11-15
  add_to_photos: "library_add",
  // 2025-02-10
  brightness_high: "brightness_7",
  brightness_low: "brightness_5",
  cloudy_snowing: "weather_snowy",
  // 2025-03-04
  nest_gale_wifi: "google_wifi",
  // 2025-04-10
  new_releases: "verified",
  emoji_events: "trophy",
  book_4_spark: "bookmark_star",
  // 2025-04-12
  app_promo: "mobile_arrow_down",
  app_shortcut: "mobile_share",
  install_mobile: "mobile_arrow_down",
  mobile_friendly: "mobile_check",
  open_in_phone: "mobile_arrow_right",
  phonelink_lock: "mobile_lock_portrait",
  phonelink_off: "mobile_off",
  stay_current_portrait: "mobile",
  stay_primary_landscape: "mobile_landscape",
  stay_primary_portrait: "mobile_alert",
  // 2025-07-03
  ad_units: "mobile_text",
  add_to_home_screen: "mobile_arrow_up_right",
  airware: "airwave",
  aod: "mobile_text_2",
  app_blocking: "mobile_block",
  book_online: "mobile_ticket",
  camera_front: "mobile_camera_front",
  camera_rear: "mobile_camera_rear",
  charging_station: "mobile_charge",
  developer_mode: "mobile_code",
  device_unknown: "mobile_question",
  dock: "mobile",
  edgesensor_high: "mobile_sensor_hi",
  edgesensor_low: "mobile_sensor_lo",
  mobile_screen_share: "mobile_share",
  nest_remote: "google_tv_remote",
  offline_share: "mobile_share_stack",
  perm_device_information: "mobile_alert",
  phone_android: "mobile_2",
  phone_iphone: "mobile_3",
  phonelink_erase: "mobile_cancel",
  phonelink_ring: "mobile_sound",
  phonelink_ring_off: "mobile_sound_off",
  phonelink_setup: "mobile_gear",
  screen_lock_landscape: "mobile_lock_landscape",
  screen_lock_portrait: "mobile_lock_portrait",
  screen_lock_rotation: "mobile_rotate_lock",
  screen_rotation: "mobile_rotate",
  screenshot: "mobile_screensaver",
  security_update_good: "mobile_check",
  security_update_warning: "mobile_alert",
  send_to_mobile: "mobile_arrow_right",
  settings_cell: "mobile_menu",
  smart_screen: "mobile_dots",
  smartphone: "mobile",
  smartphone_camera: "mobile_camera",
  stay_current_landscape: "mobile_landscape",
  stream_apps: "mobile_chat",
  system_update: "mobile_arrow_down",
  tap_and_play: "mobile_cast",
  vibration: "mobile_vibrate",
  // 2025-07-17
  filter_hdr: "landscape",
  // 2025-08-09
  nights_stay: "partly_cloudy_night",
  // 2025-08-20
  sign_language_2: "sign_language",
  // 2025-09-19
  motion_photos_off: "motion_sensor_idle",
  transcribe: "record_voice_over",
  // 2025-10-16
  battery_saver: "battery_plus",
  // 2025-10-30
  heart_arrow: "favorite",
  // 2025-11-21
  volume_down_alt: "volume_down",
};

export type IconProps = {
  icon: string;
} & Pick<
  MaterialIconProps,
  "fill" | "color" | "fontSize" | "className" | "onClick" 
>;

type MaterialIconName = MaterialIconProps["icon"];

const findMaterialIcon = (icon: string) => {
  let found: MaterialIconName | undefined;

  if (materialIconNames.has(icon)) found = icon as MaterialIconName;
  if (materialIconsRemoved[icon]) {
    if (
      process.env.NODE_ENV !== "production" &&
      !logsMissingIcons.includes(icon)
    ) {
      console.log("Deprecated icon : " + icon);
      logsMissingIcons.push(icon);
    }
    found = materialIconsRemoved[icon];
  }

  return found;
};

const findBootstrapIcon = (icon: string) => {
  return bootstrapIconNames.has(icon) ? icon as BootstrapIconName : undefined;
};

const logsMissingIcons: Array<string> = [];

export const Icon = forwardRef<HTMLElement, IconProps>(
  ({ icon, ...props }, ref) => {
    const mi = findMaterialIcon(icon);
    const bi = findBootstrapIcon(icon);

    if (mi) return <MaterialIcon icon={mi} {...props} ref={ref} />;
    if (bi) return <BootstrapIcon icon={bi} {...props} />;

    if (
      process.env.NODE_ENV !== "production" &&
      !logsMissingIcons.includes(icon)
    ) {
      console.log("Unknown icon : " + icon);
      logsMissingIcons.push(icon);
    }

    return <MaterialIcon icon="apps" {...props} ref={ref} />;
  },
);
