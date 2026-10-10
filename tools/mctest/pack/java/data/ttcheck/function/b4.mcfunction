# [B4] armor.* 是否存在 | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s armor.head with minecraft:diamond_helmet 1
execute if items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #b4s tt 1
execute unless items entity @s armor.head minecraft:diamond_helmet run scoreboard players set #b4s tt 0
execute if items entity @s armor.* minecraft:diamond_helmet run scoreboard players set #b4 tt 1
execute unless items entity @s armor.* minecraft:diamond_helmet run scoreboard players set #b4 tt 0
