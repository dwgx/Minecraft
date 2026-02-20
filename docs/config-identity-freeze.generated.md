# Config Identity Freeze Snapshot

Generated at: 2026-02-20 12:17:48

## Modules

| id | name | category | source | setting keys |
| --- | --- | --- | --- | --- |
| auto_armor | AutoArmor | WORLD | "src\client\module\impl\world\AutoArmorModule.java" | min_delay_ms, max_delay_ms, open_inventory_only, prefer_durability |
| auto_clicker | AutoClicker | COMBAT | "src\client\module\impl\combat\AutoClickerModule.java" | require_attack_key, weapon_only, randomize_cps, min_cps, max_cps |
| auto_tool | AutoTool | WORLD | "src\client\module\impl\world\AutoToolModule.java" | require_attack_key, switch_back |
| click_gui | ClickGUI | CLIENT | "src\client\module\impl\client\ClickGuiModule.java" | palette, corner_radius, panel_alpha, backdrop, backdrop_alpha, accent_override_enabled, accent_override, ui_anim_enabled, ui_anim_speed, ui_anim_smooth, ui_control_anim_speed, ui_slider_anim_enabled, ui_slider_anim_speed, ui_page_anim_speed, ui_list_anim_speed, ui_selection_anim_speed, ui_input_anim_speed, ui_input_anim_smooth, ui_anim_type, ui_input_anim_type, ui_open_anim_type, ui_close_anim_type, ui_switch_anim_type, ui_back_anim_type, last_category_ordinal, last_module_index, last_category_scroll, last_module_scroll, last_setting_scroll, last_compat_setting_scroll, setting_center_scale, setting_center_anchor_x, setting_center_anchor_y, setting_center_picker_x, setting_center_picker_y |
| eagle | Eagle | MOVEMENT | "src\client\module\impl\movement\EagleModule.java" | only_ground, only_moving, disable_flying, sample_depth, sample_inset |
| hud_edit | HudEdit | HUD | "src\client\module\impl\client\HudEditModule.java" |  |
| inventory_move | InventoryMove | MISC | "src\client\module\impl\misc\InventoryMoveModule.java" | allow_jump, allow_sneak, allow_sprint, allow_in_chat |
| keep_sprint | KeepSprint | MOVEMENT | "src\client\module\impl\movement\KeepSprintModule.java" | air_sprint, use_item_sprint |
| killaura | KillAura | COMBAT | "src\client\module\impl\combat\KillAuraModule.java" | attack_mode, sort_mode, rotation_mode, aim_mode, auto_block_mode, attack_while_scaffold, keep_sprint, no_swing, ray_cast, randomize_cps, right_click_only, min_cps, max_cps, switch_delay_ticks, multi_targets, range, wall_range, field_of_view, turn_speed, block_range |
| noslow | NoSlow | MOVEMENT | "src\client\module\impl\movement\NoSlowModule.java" | forward_boost, strafe_boost, sprint_assist, only_while_moving, skip_bow |
| scaffold | Scaffold | MOVEMENT | "src\client\module\impl\movement\ScaffoldModule.java" | preset, only_moving, auto_switch, swing_hand, slot_selection, place_mode, extension, omni_directional_expand, search_radius, place_delay, place_delay_jitter, extra_place_attempts, look_ahead_distance, adaptive_prediction, prediction_gain, stability_bias, min_move_speed, bridge_profile, aim_mode, rotation_mode, strict_aim, aim_tolerance, turn_speed, raycast_mode, keep_y, same_y_mode, downwards_mode, tower_mode, sprint_mode, safe_edge_sneak, air_safe_edge, eagle_mode, eagle_blocks, eagle_ticks, eagle_edge_distance |
| sprint | Sprint | MOVEMENT | "src\client\module\impl\movement\SprintModule.java" | mode, in_air, while_using_item, ignore_blindness, ignore_hunger |
| stealer | Stealer | WORLD | "src\client\module\impl\world\StealerModule.java" | min_delay_ms, max_delay_ms, close_after_empty_checks, useful_only, auto_close, chest_title_guard |
| target_panel | TargetPanel | HUD | "src\client\module\impl\render\TargetPanelModule.java" | x, y, width, height, show_distance, show_health_bar |
| ui_scale_edit | UIScaleEdit | CLIENT | "src\client\module\impl\client\UiScaleEditModule.java" | edit_target, click_gui_scale, click_gui_motion, click_gui_anchor_x, click_gui_anchor_y, hud_edit_scale, hud_edit_motion, hud_edit_anchor_x, hud_edit_anchor_y, ui_anim_enabled, ui_anim_speed, ui_anim_smooth, ui_anim_type |
| velocity | Velocity | COMBAT | "src\client\module\impl\combat\VelocityModule.java" | horizontal_percent, vertical_percent, chance_percent, only_when_moving, only_on_ground |

## HUD Elements

| id | name | source | setting keys |
| --- | --- | --- | --- |
| hud_fps | FPS | "src\client\hud\HudFpsElement.java" | fps_source |

## Enum Sources (persisted names must stay stable when serialized)

| enum | source |
| --- | --- |
| AimMode | "src\client\module\impl\combat\KillAuraModule.java" |
| Anchor | "src\client\hud\Anchor.java" |
| AttackMode | "src\client\module\impl\combat\KillAuraModule.java" |
| AutoBlockMode | "src\client\module\impl\combat\KillAuraModule.java" |
| BridgeProfile | "src\client\module\impl\movement\ScaffoldModule.java" |
| Dock | "src\client\hud\Dock.java" |
| DownwardsMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| EagleMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| FpsSource | "src\client\hud\HudFpsElement.java" |
| HudLayer | "src\client\hud\HudLayer.java" |
| Mode | "src\client\module\impl\movement\SprintModule.java" |
| RaycastMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| RotationMode | "src\client\module\impl\combat\KillAuraModule.java" |
| SameYMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| ScaffoldAimMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| ScaffoldPlaceMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| ScaffoldPreset | "src\client\module\impl\movement\ScaffoldModule.java" |
| ScaffoldRotationMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| SlotSelectionMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| SortMode | "src\client\module\impl\combat\KillAuraModule.java" |
| SprintMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| TowerMode | "src\client\module\impl\movement\ScaffoldModule.java" |
| UiTarget | "src\client\module\impl\client\UiScaleEditModule.java" |
