# [V4] count:2b 对有2个的 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 2
execute if items entity @s hotbar.5 minecraft:diamond[count=2] run scoreboard players set #v4s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond[count=2] run scoreboard players set #v4s tt 0
execute if data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond",count:2b}]} run scoreboard players set #v4 tt 1
execute unless data entity @s {Inventory:[{Slot:5b,id:"minecraft:diamond",count:2b}]} run scoreboard players set #v4 tt 0
