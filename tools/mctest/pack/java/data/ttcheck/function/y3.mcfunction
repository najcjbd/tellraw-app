# [Y3] 老式 tag:{Damage:3} 在 26.2 应不命中（再次确认 tag 已失效） | 期望不匹配
scoreboard objectives add tt dummy
clear @s
item replace entity @s weapon.mainhand with minecraft:diamond_sword[minecraft:damage=3] 1
execute if items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y3s tt 1
execute unless items entity @s weapon.mainhand minecraft:diamond_sword[damage=3] run scoreboard players set #y3s tt 0
execute if data entity @s {SelectedItem:{id:"minecraft:diamond_sword",tag:{Damage:3}}} run scoreboard players set #y3 tt 1
execute unless data entity @s {SelectedItem:{id:"minecraft:diamond_sword",tag:{Damage:3}}} run scoreboard players set #y3 tt 0
