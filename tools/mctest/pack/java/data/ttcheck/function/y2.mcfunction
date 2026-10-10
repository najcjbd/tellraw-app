# [Y2] 改写后的组件写法（custom_data/damage）应命中 | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword[minecraft:damage=3] 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y2s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y2s tt 0
execute if data entity @s {SelectedItem:{id:"minecraft:diamond_sword",components:{"minecraft:damage":3}}} run scoreboard players set #y2 tt 1
execute unless data entity @s {SelectedItem:{id:"minecraft:diamond_sword",components:{"minecraft:damage":3}}} run scoreboard players set #y2 tt 0
