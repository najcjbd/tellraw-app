# [Y5] 损耗3的剑：data=0 应不命中 | 期望不匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.weapon.mainhand 0 diamond_sword 1 3
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=3}] run scoreboard players set #y5s tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=3}] run scoreboard players set #y5s tt 0
execute if entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=0}] run scoreboard players set #y5 tt 1
execute unless entity @s[hasitem={item=diamond_sword,location=slot.weapon.mainhand,data=0}] run scoreboard players set #y5 tt 0
