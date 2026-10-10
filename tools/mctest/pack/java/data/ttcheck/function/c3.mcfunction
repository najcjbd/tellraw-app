# [C3] 玩家主手 = SelectedItem | 期望匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #c3s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword run scoreboard players set #c3s tt 0
execute if data entity @s {SelectedItem:{id:"minecraft:diamond_sword"}} run scoreboard players set #c3 tt 1
execute unless data entity @s {SelectedItem:{id:"minecraft:diamond_sword"}} run scoreboard players set #c3 tt 0
