# [B5] 裸 *：应为假（* 不存在->报错） | 只看实测值
scoreboard objectives add tt dummy
clear @s
item replace entity @s hotbar.5 with minecraft:diamond 1
execute if items entity @s hotbar.5 minecraft:diamond run scoreboard players set #b5s tt 1
execute unless items entity @s hotbar.5 minecraft:diamond run scoreboard players set #b5s tt 0
execute if items entity @s * minecraft:diamond run scoreboard players set #b5 tt 1
execute unless items entity @s * minecraft:diamond run scoreboard players set #b5 tt 0
