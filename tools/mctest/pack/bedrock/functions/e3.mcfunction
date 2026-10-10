# [E3] 副手 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.weapon.offhand 0 diamond_sword 1
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.offhand}] run scoreboard players set #e3s tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.offhand}] run scoreboard players set #e3s tt 0
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.offhand}] run scoreboard players set #e3 tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.offhand}] run scoreboard players set #e3 tt 0
