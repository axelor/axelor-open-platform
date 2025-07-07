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
 * Map removed material icons with their equivalency in order to maintained compatibility
 * From 2024-05-14 to 2025-03-21 : see https://material-symbols-changelog.vercel.app/
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
  pixel_9_pro_fold: "phone_android",
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
  nest_gale_wifi: "nest_wifi_router",
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
